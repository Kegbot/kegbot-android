package org.kegbot.kegtap;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.proto.Api.SystemEventSet;
import org.kegbot.proto.Models.SystemEvent;

import android.app.ListFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Displays events in a System Event Set in a list.
 *
 * @author justinkoh
 */
public class EventListFragment extends ListFragment {

  private static final String LOG_TAG = "EventList";
  
  private ArrayAdapter<SystemEvent> mAdapter;
  private KegbotApi mApi;

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mAdapter = new ArrayAdapter<SystemEvent>(getActivity(), R.layout.event_list_item, R.id.title) {

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView title = (TextView) view.findViewById(R.id.title);
        TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
        return view;
      }
      
    };
    setListAdapter(mAdapter);
  }
  
  void setKegbotApi(KegbotApi api) {
    mApi = api;
  }
  
  void loadEvents() {
    new EventLoaderTask().execute();
  }
  
  private class EventLoaderTask extends AsyncTask<Void, Void, SystemEventSet> {

    @Override
    protected SystemEventSet doInBackground(Void... params) {
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
    protected void onPostExecute(SystemEventSet result) {
      mAdapter.clear();
      if (result != null) {
        mAdapter.addAll(result.getEventsList());
        setListShown(true);
      }
    }
  }
}
