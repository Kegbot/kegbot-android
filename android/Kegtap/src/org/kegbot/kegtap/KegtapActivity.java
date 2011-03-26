package org.kegbot.kegtap;

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