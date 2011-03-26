package org.kegbot.kegtap;

import org.apache.http.impl.client.DefaultHttpClient;
import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiImpl;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class KegtapActivity extends Activity {

  public final String LOG_TAG = "KegtapActivity";

  private KegbotApi mApi;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    Log.w(LOG_TAG, "Main view!");
    mApi = new KegbotApiImpl(new DefaultHttpClient(), "http://kegbot.net/sfo/api");
    TapListFragment taps = (TapListFragment) getFragmentManager().findFragmentById(R.id.tap_list);
    taps.setKegbotApi(mApi);
    taps.loadTaps();
    EventListFragment events = (EventListFragment) getFragmentManager().findFragmentById(
        R.id.event_list);
    events.setKegbotApi(mApi);
    events.loadEvents();
  }
}