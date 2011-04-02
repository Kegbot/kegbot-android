package org.kegbot.kegtap;

import java.text.ParseException;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.kegtap.util.image.ImageDownloader;
import org.kegbot.proto.Api.SystemEventDetail;
import org.kegbot.proto.Api.SystemEventDetailSet;
import org.kegbot.proto.Models.Image;
import org.kegbot.proto.Models.SystemEvent;

import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Displays events in a System Event Set in a list.
 *
 * @author justinkoh
 */
public class EventListFragment extends ListFragment {

  private static final String LOG_TAG = "EventList";

  private ArrayAdapter<SystemEventDetail> mAdapter;
  private KegbotApi mApi;
  private ImageDownloader mImageDownloader;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mAdapter = new ArrayAdapter<SystemEventDetail>(getActivity(), R.layout.event_list_item,
        R.id.eventTitle) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        final SystemEventDetail eventDetail = getItem(position);
        final SystemEvent event = eventDetail.getEvent();
        View view = super.getView(position, convertView, parent);
        TextView title = (TextView) view.findViewById(R.id.eventTitle);
        title.setText(getTitle(event));

        TextView subtitle = (TextView) view.findViewById(R.id.eventSubtitle);
        String isoTime = event.getTime();
        try {
          long time = Utils.dateFromIso8601String(isoTime);

          CharSequence relTime = DateUtils.getRelativeDateTimeString(getContext(), time, 60 * 1000,
              7 * 24 * 60 * 60 * 100, 0);
          subtitle.setText(relTime);
        } catch (ParseException e) {
          // Pass
        }

        ImageView icon = (ImageView) view.findViewById(R.id.eventIcon);
        if (icon != null && eventDetail.hasImage()) {
          final Image image = eventDetail.getImage();
          final String imageUrl = image.getUrl();
          mImageDownloader.download(imageUrl, icon);
        }
        return view;
      }

    };
    setListAdapter(mAdapter);
  }

  void setKegbotApi(KegbotApi api) {
    mApi = api;
  }

  void setImageDownloader(ImageDownloader imageDownloader) {
    mImageDownloader = imageDownloader;
  }

  private String getTitle(SystemEvent event) {
    final String kind = event.getKind();
    String result;
    if ("keg_ended".equals(kind)) {
      result = "Keg " + event.getKegId() + " ended";
    } else if ("drink_poured".equals(kind)) {
      if (event.hasUserId()) {
        result = event.getUserId() + " poured a drink";
      } else {
        result = "guest poured a drink";
      }
    } else if ("session_joined".equals(kind)) {
      result = event.getUserId() + " started drinking";
    } else {
      result = "Unkown event.";
    }
    return result;
  }

  void loadEvents() {
    new EventLoaderTask().execute();
  }

  private class EventLoaderTask extends AsyncTask<Void, Void, SystemEventDetailSet> {

    @Override
    protected SystemEventDetailSet doInBackground(Void... params) {
      try {
        return mApi.getRecentEvents();
      } catch (KegbotApiException e) {
        Log.w(LOG_TAG, "Could not load events.", e);
        return null;
      }
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      setListShown(false);
    }

    @Override
    protected void onPostExecute(SystemEventDetailSet result) {
      mAdapter.clear();
      if (result != null) {
        mAdapter.addAll(result.getEventsList());
        setListShown(true);
      }
    }
  }
}
