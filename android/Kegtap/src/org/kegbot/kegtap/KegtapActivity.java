package org.kegbot.kegtap;

import org.apache.http.impl.client.DefaultHttpClient;
import org.kegbot.api.KegbotApiImpl;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class KegtapActivity extends Activity {

  public final String LOG_TAG = "KegtapActivity";
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    Log.w(LOG_TAG, "Main view!");
    TapListFragment taps = (TapListFragment) getFragmentManager().findFragmentById(R.id.tap_list);
    taps.setKegbotApi(new KegbotApiImpl(new DefaultHttpClient(), getApiUrl()));
    taps.loadTaps();
    EventListFragment events = (EventListFragment) getFragmentManager().findFragmentById(
        R.id.event_list);
    events.setKegbotApi(new KegbotApiImpl(new DefaultHttpClient(), getApiUrl()));
    events.loadEvents();
  }
  
  private String getApiUrl() {
    return "http://kegbot.net/sfo/api";
  }
}