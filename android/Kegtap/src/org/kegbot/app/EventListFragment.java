/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app;

import java.text.ParseException;
import java.util.Comparator;
import java.util.List;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.util.image.ImageDownloader;
import org.kegbot.proto.Models.Drink;
import org.kegbot.proto.Models.SystemEvent;

import android.app.Activity;
import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
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

  private ArrayAdapter<SystemEvent> mAdapter;
  private final KegbotApi mApi = KegbotApiImpl.getSingletonInstance();
  private ImageDownloader mImageDownloader;

  private int mLastEventId = -1;

  private static Comparator<SystemEvent> EVENTS_DESCENDING = new Comparator<SystemEvent>() {
    @Override
    public int compare(SystemEvent object1, SystemEvent object2) {
      try {
        final long time1 = Utils.dateFromIso8601String(object1.getTime());
        final long time2 = Utils.dateFromIso8601String(object2.getTime());
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

    mAdapter = new ArrayAdapter<SystemEvent>(getActivity(), R.layout.event_list_item,
        R.id.eventTitle) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        final SystemEvent eventDetail = getItem(position);
        final View view = super.getView(position, convertView, parent);
        try {
          formatEvent(eventDetail, view);
        } catch (Throwable e) {
          Log.wtf(LOG_TAG, "UNCAUGHT EXCEPTION", e);
        }
        return view;
      }

      private void formatEvent(SystemEvent event, View view) {
        // Common: image.
        final ImageView icon = (ImageView) view.findViewById(R.id.eventIcon);
        icon.setVisibility(View.GONE);
        if (event.hasImage()) {
          final String imageUrl = event.getImage().getThumbnailUrl();
          if (!Strings.isNullOrEmpty(imageUrl)) {
            icon.setVisibility(View.VISIBLE);
            icon.setImageBitmap(null);

            // Default to unknown drinker, may be immediately replaced by downloader.
            icon.setBackgroundResource(R.drawable.unknown_drinker);
            mImageDownloader.download(imageUrl, icon);
          }
        }

        // Common: user portion.
        final TextView userName = (TextView) view.findViewById(R.id.eventUserName);
        final String userNameString = getUsernameForEvent(event);
        if (userNameString != null) {
          userName.setText(userNameString);
          userName.setVisibility(View.VISIBLE);
        } else {
          userName.setVisibility(View.GONE);
        }

        // Event body.
        final TextView title = (TextView) view.findViewById(R.id.eventTitle);
        title.setText(getTitle(event));

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

      private String getTitle(SystemEvent eventDetail) {
        final String kind = eventDetail.getKind();
        final Drink drink = eventDetail.getDrink();
        String result;

        if ("keg_ended".equals(kind)) {
          result = "ended";
        } else if ("keg_tapped".equals(kind)) {
          result = "tapped";
        } else if ("drink_poured".equals(kind)) {
          double ounces = Units.volumeMlToOunces(drink.getVolumeMl());
          result = String.format("poured %.1f ounces", Double.valueOf(ounces));
        } else if ("session_joined".equals(kind)) {
          result = "started drinking";
        } else if ("session_started".equals(kind)) {
          result = "started a new session";
        } else {
          result = "Unknown event";
        }
        return result;
      }

      private String getUsernameForEvent(SystemEvent eventDetail) {
        final String userName;
        if (!eventDetail.hasUser()) {
          userName = "a guest";
        } else {
          userName = eventDetail.getUser().getUsername();
        }

        final String kind = eventDetail.getKind();
        if ("drink_poured".equals(kind)) {
          return userName;
        } else if ("session_joined".equals(kind)) {
          return userName;
        } else if ("session_started".equals(kind)) {
          return userName;
        } else if ("keg_ended".equals(kind) || "keg_tapped".equals(kind)) {
          if (eventDetail.hasKeg()) {
            return "Keg " + eventDetail.getKeg().getId();
          }
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

  private class EventLoaderTask extends AsyncTask<Void, Void, List<SystemEvent>> {

    @Override
    protected List<SystemEvent> doInBackground(Void... params) {
      try {
        if (mLastEventId <= 0) {
          return mApi.getRecentEvents();
        } else {
          return mApi.getRecentEvents(mLastEventId);
        }
      } catch (KegbotApiException e) {
        Log.w(LOG_TAG, "Could not load events: " + e.toString());
        return null;
      }
    }

    @Override
    protected void onPostExecute(List<SystemEvent> events) {
      int greatestEventId = mLastEventId;
      if (events != null) {
        for (final SystemEvent event : events) {
          mAdapter.add(event);
          final int eventId = event.getId();
          if (eventId > greatestEventId) {
            greatestEventId = eventId;
          }
        }
        if (greatestEventId != mLastEventId) {
          mLastEventId = greatestEventId;
          Log.d(LOG_TAG, "Events reloaded, most recent event id: " + mLastEventId);
        }
        if (!events.isEmpty()) {
          mAdapter.sort(EVENTS_DESCENDING);
        }
      }
    }
  }
}
