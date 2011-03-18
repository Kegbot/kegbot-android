package org.kegbot.kegtap;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;

import retrofit.http.RestAdapter;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

public class KegtapActivity extends Activity {

  public final String LOG_TAG = "KegtapActivity";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    Log.w(LOG_TAG, "Main view!");
    
    EventListAdapter adapter = new EventListAdapter(this);
    Log.w(LOG_TAG, "Generated adapter.");
    ListView eventList = (ListView) findViewById(R.id.drinkList);
    Log.w(LOG_TAG, "Found event list");
    eventList.setAdapter(adapter);
    Log.w(LOG_TAG, "Set adapter");
  }
}