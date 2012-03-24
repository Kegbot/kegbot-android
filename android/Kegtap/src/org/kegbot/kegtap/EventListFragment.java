package org.kegbot.kegtap;

import java.text.ParseException;
import java.util.Comparator;
import java.util.List;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.kegtap.util.image.ImageDownloader;
import org.kegbot.proto.Api.SystemEventDetail;
import org.kegbot.proto.Api.SystemEventDetailSet;
import org.kegbot.proto.Models.SystemEvent;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Strings;

/**
 * Displays events in a System Event Set in a list.
 *
 * @author justinkoh
 */
public class EventListFragment extends ListFragment {

  private static final String LOG_TAG = EventListFragment.class.getSimpleName();

  private ArrayAdapter<SystemEventDetail> mAdapter;
  private final KegbotApi mApi = KegbotApiImpl.getSingletonInstance();
  private ImageDownloader mImageDownloader;

  private long mLastEventId = -1;

  private static Comparator<SystemEventDetail> EVENTS_DESCENDING = new Comparator<SystemEventDetail>() {
    @Override
    public int compare(SystemEventDetail object1, SystemEventDetail object2) {
      try {
        final long time1 = Utils.dateFromIso8601String(object1.getEvent().getTime());
        final long time2 = Utils.dateFromIso8601String(object2.getEvent().getTime());
        return Long.valueOf(time2).compareTo(Long.valueOf(time1));
      } catch (ParseException e) {
        Log.wtf(LOG_TAG, "Error parsing times", e);
        return 0;
      }
    }
  };

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mImageDownloader = ImageDownloader.getSingletonInstance(activity);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    mAdapter = new ArrayAdapter<SystemEventDetail>(getActivity(), R.layout.event_list_item,
        R.id.eventTitle) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        final SystemEventDetail eventDetail = getItem(position);
        final View view = super.getView(position, convertView, parent);
        try {
          formatEvent(eventDetail, view);
        } catch (Throwable e) {
          Log.wtf(LOG_TAG, "UNCAUGHT EXCEPTION", e);
        }
        return view;
      }

      private void formatEvent(SystemEventDetail eventDetail, View view) {
        Log.i(LOG_TAG, "Formatting event:" + eventDetail.getEvent().getId());
        final SystemEvent event = eventDetail.getEvent();
        // Common: image.
        final ImageView icon = (ImageView) view.findViewById(R.id.eventIcon);
        final String imageUrl = eventDetail.getImage().getUrl();
        if (!Strings.isNullOrEmpty(imageUrl)) {
          icon.setVisibility(View.VISIBLE);
          icon.setImageBitmap(null);

          // Default to unknown drinker, may be immediately replaced by downlaoder.
          icon.setBackgroundResource(R.drawable.unknown_drinker);
          mImageDownloader.download(imageUrl, icon);
        } else {
          icon.setVisibility(View.GONE);
        }

        // Common: user portion.
        final TextView userName = (TextView) view.findViewById(R.id.eventUserName);
        final String userNameString = getUsernameForEvent(eventDetail);
        if (userNameString != null) {
          userName.setText(userNameString);
          userName.setVisibility(View.VISIBLE);
        } else {
          userName.setVisibility(View.GONE);
        }

        // Event body.
        final TextView title = (TextView) view.findViewById(R.id.eventTitle);
        title.setText(getTitle(eventDetail.getEvent()));

        // Date and time.
        TextView dateView = (TextView) view.findViewById(R.id.eventDate);
        String isoTime = event.getTime();
        try {
          long time = Utils.dateFromIso8601String(isoTime);

          CharSequence relTime = DateUtils.getRelativeDateTimeString(getContext(), time, 60 * 1000,
              7 * 24 * 60 * 60 * 100, 0);
          dateView.setText(relTime);
          dateView.setVisibility(View.VISIBLE);
        } catch (ParseException e) {
          dateView.setVisibility(View.GONE);
        }

      }

      private String getTitle(SystemEvent event) {
        final String kind = event.getKind();
        String result;

        if ("keg_ended".equals(kind)) {
          result = "ended";
        } else if ("keg_tapped".equals(kind)) {
          result = "tapped";
        } else if ("drink_poured".equals(kind)) {
          result = "poured a drink";
        } else if ("session_joined".equals(kind)) {
          result = "started drinking";
        } else if ("session_started".equals(kind)) {
          result = "started a new session";
        } else {
          result = "Unknown event";
        }
        return result;
      }

      private String getUsernameForEvent(SystemEventDetail eventDetail) {
        final SystemEvent event = eventDetail.getEvent();
        String userName = event.getUserId();
        if (Strings.isNullOrEmpty(userName)) {
          userName = "a guest";
        }

        final String kind = eventDetail.getEvent().getKind();
        if ("drink_poured".equals(kind)) {
          return userName;
        } else if ("session_joined".equals(kind)) {
          return userName;
        } else if ("session_started".equals(kind)) {
          return userName;
        } else if ("keg_ended".equals(kind)) {
          return "Keg " + event.getKegId();
        } else if ("keg_tapped".equals(kind)) {
          return "Keg " + event.getKegId();
        }

        return null;
      }


    };

    AnimationSet set = new AnimationSet(true);

    Animation animation = new AlphaAnimation(0.0f, 1.0f);
    animation.setDuration(300);
    set.addAnimation(animation);
    animation = new TranslateAnimation(
        Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f
    );
    animation.setDuration(300);
    set.addAnimation(animation);

    LayoutAnimationController controller = new LayoutAnimationController(set, 0.3f);
    getListView().setLayoutAnimation(controller);
    setListAdapter(mAdapter);
  }

  void loadEvents() {
    new EventLoaderTask().execute();
  }

  private class EventLoaderTask extends AsyncTask<Void, Void, SystemEventDetailSet> {

    @Override
    protected SystemEventDetailSet doInBackground(Void... params) {
      try {
        if (mLastEventId <= 0) {
          return mApi.getRecentEvents();
        } else {
          return mApi.getRecentEvents(mLastEventId);
        }
      } catch (KegbotApiException e) {
        Log.w(LOG_TAG, "Could not load events.", e);
        return null;
      }
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected void onPostExecute(SystemEventDetailSet result) {
      Log.d(LOG_TAG, "Events reloaded.");
      if (result != null) {
        final List<SystemEventDetail> events = result.getEventsList();
        for (final SystemEventDetail event : events) {
          mAdapter.add(event);
          final long eventId = Long.valueOf(event.getEvent().getId()).longValue();
          if (eventId > mLastEventId) {
            mLastEventId = eventId;
          }
        }
        if (!events.isEmpty()) {
          mAdapter.sort(EVENTS_DESCENDING);
        }
      }
    }
  }
}
