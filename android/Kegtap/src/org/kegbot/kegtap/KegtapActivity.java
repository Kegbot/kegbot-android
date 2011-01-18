package org.kegbot.kegtap;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

import retrofit.http.RestAdapter;
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
  }
}