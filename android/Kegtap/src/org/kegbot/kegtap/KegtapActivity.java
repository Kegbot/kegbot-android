package org.kegbot.kegtap;

import org.apache.http.impl.client.DefaultHttpClient;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.kegtap.service.KegbotCoreService;
import org.kegbot.kegtap.util.image.ImageDownloader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class KegtapActivity extends Activity {

  public final String LOG_TAG = "KegtapActivity";

  private final ImageDownloader mImageDownloader = new ImageDownloader();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    Log.w(LOG_TAG, "Main view!");
    TapListFragment taps = (TapListFragment) getFragmentManager().findFragmentById(R.id.tap_list);
    taps.setKegbotApi(new KegbotApiImpl(new DefaultHttpClient(), getApiUrl()));
    taps.setImageDownloader(mImageDownloader);
    taps.loadTaps();

    EventListFragment events = (EventListFragment) getFragmentManager().findFragmentById(
        R.id.event_list);
    events.setKegbotApi(new KegbotApiImpl(new DefaultHttpClient(), getApiUrl()));
    events.setImageDownloader(mImageDownloader);
    events.loadEvents();

    Intent intent = new Intent(this, KegbotCoreService.class);
    startService(intent);

  }

  private String getApiUrl() {
    // return "http://oldgertie.kegbot.net/api";
    return "http://kegbot.net/sfo/api";
  }
}