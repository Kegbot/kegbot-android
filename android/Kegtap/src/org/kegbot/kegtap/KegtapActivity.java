package org.kegbot.kegtap;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.kegtap.service.KegboardService;
import org.kegbot.kegtap.util.PreferenceHelper;
import org.kegbot.kegtap.util.image.ImageDownloader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.common.base.Strings;

public class KegtapActivity extends CoreActivity {

  public final String LOG_TAG = "KegtapActivity";

  private final ImageDownloader mImageDownloader = ImageDownloader.getSingletonInstance();

  private TapStatusFragment mTapStatus;

  private EventListFragment mEvents;

  private ControlsFragment mControls;

  private SessionStatsFragment mSession;

  private SharedPreferences mPreferences;
  private PreferenceHelper mPrefsHelper;

  private final Handler mHandler = new Handler();

  private final OnSharedPreferenceChangeListener mPreferenceListener =
      new OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      if (PreferenceHelper.KEY_SELECTED_KEGBOT.equals(key)) {
        initializeUi();
      }
    }
  };

  private final OnClickListener mOnBeerMeClickedListener =
    new OnClickListener() {
      @Override
      public void onClick(View v) {
        final Intent intent = new Intent(KegtapActivity.this, DrinkerSelectActivity.class);
        startActivity(intent);
      }
    };

  private final Runnable mRefreshRunnable = new Runnable() {
    @Override
    public void run() {
      Log.d(LOG_TAG, "Reloading events.");
      mEvents.loadEvents();
      mHandler.postDelayed(this, 10000);
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);

    mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    mPrefsHelper = new PreferenceHelper(mPreferences);

    mTapStatus = (TapStatusFragment) getFragmentManager().findFragmentById(
        R.id.tap_status);

    mEvents = (EventListFragment) getFragmentManager().findFragmentById(
        R.id.event_list);

    mControls = (ControlsFragment) getFragmentManager().findFragmentById(
        R.id.controls);

    mSession = (SessionStatsFragment) getFragmentManager().findFragmentById(
        R.id.currentSessionFragment);

    ((Button) findViewById(R.id.beerMeButton)).setOnClickListener(mOnBeerMeClickedListener);

    View v = findViewById(R.id.tap_status);
    v.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
  }

  @Override
  public void onStart() {
    super.onStart();
    mPreferences.registerOnSharedPreferenceChangeListener(mPreferenceListener);
    initializeUi();
  }

  @Override
  protected void onResume() {
    super.onResume();
    handleIntent();
    initializeUi();
    mHandler.postDelayed(mRefreshRunnable, 10000);
  }

  @Override
  protected void onPause() {
    mHandler.removeCallbacks(mRefreshRunnable);
    super.onPause();
  }

  @Override
  public void onStop() {
    super.onStop();
    mPreferences.unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
    case R.id.settings:
      SettingsActivity.startSettingsActivity(this);
      return true;
    case android.R.id.home:
      // TODO: navigate up
      return true;
    default:
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntent();
  }

  private void handleIntent() {
    final Intent intent = getIntent();
    final String action = intent.getAction();
    Log.d(LOG_TAG, "Handling intent: " + intent);
    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
      final Intent serviceIntent = new Intent(this, KegboardService.class);
      serviceIntent.setAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
      startService(serviceIntent);
    }
  }

  /**
   * Starts the UI.
   * <p>
   * Checks if there is a current kegbot set up: if not, launches settings.
   * <p>
   * If so, loads from last known kegbot.
   */
  private void initializeUi() {
    String username = mPrefsHelper.getUsername();
    if (Strings.isNullOrEmpty(username)) {
      SettingsActivity.startSettingsActivity(this);
    } else {
      //getActionBar().setTitle(mPrefsHelper.getKegbotName());
      updateApiUrl(mPrefsHelper.getKegbotUrl());
    }
  }

  private void updateApiUrl(Uri apiUrl) {
    KegbotApi api = KegbotApiImpl.getSingletonInstance();
    String username = mPrefsHelper.getUsername();
    String password = mPrefsHelper.getPassword();
    api.setAccountCredentials(username, password);
    mTapStatus.setKegbotApi(api);
    api.setApiUrl(apiUrl.toString());
    loadUiFragments();
  }

  private void loadUiFragments() {
    mTapStatus.loadTap();
    //mEvents.loadEvents();
    mSession.loadCurrentSessionDetail();
  }
}
