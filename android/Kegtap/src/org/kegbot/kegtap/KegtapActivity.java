package org.kegbot.kegtap;

import org.apache.http.impl.client.DefaultHttpClient;
import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.kegtap.service.KegboardService;
import org.kegbot.kegtap.util.KegbotDescriptor;
import org.kegbot.kegtap.util.PreferenceUtils;
import org.kegbot.kegtap.util.image.ImageDownloader;

import android.app.ActionBar;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class KegtapActivity extends CoreActivity {

  public final String LOG_TAG = "KegtapActivity";

  private final ImageDownloader mImageDownloader = new ImageDownloader();

  private TapStatusFragment mTapStatus;

  private EventListFragment mEvents;

  private ControlsFragment mControls;

  private SharedPreferences mPreferences;

  private final OnSharedPreferenceChangeListener mPreferenceListener =
      new OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      if (PreferenceUtils.SELECTED_KEGBOT_KEY.equals(key)) {
        initializeUi();
      }
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    setupActionBar();
    bindToCoreService();

    mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    mTapStatus = (TapStatusFragment) getFragmentManager().findFragmentById(
        R.id.tap_status);
    mTapStatus.setImageDownloader(mImageDownloader);

    mEvents = (EventListFragment) getFragmentManager().findFragmentById(
        R.id.event_list);
    mEvents.setImageDownloader(mImageDownloader);

    mControls = (ControlsFragment) getFragmentManager().findFragmentById(
        R.id.controls);
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
      launchSettings();
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
  private void setupActionBar() {
    ActionBar actionBar = getActionBar();
    actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.header_bg_square));
    actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO);
    actionBar.setTitle("");
  }

  private void launchSettings() {
    Intent intent = new Intent(this, SettingsActivity.class);
    startActivity(intent);
  }

  /**
   * Starts the UI.
   * <p>
   * Checks if there is a current kegbot set up: if not, launches settings.
   * <p>
   * If so, loads from last known kegbot.
   */
  private void initializeUi() {
    String kegbotUrl = PreferenceUtils.getKegbotUrl(mPreferences);
    if (TextUtils.isEmpty(kegbotUrl)) {
      launchSettings();
    } else {
      getActionBar().setTitle(PreferenceUtils.getKegbotName(mPreferences));
      updateApiUrl(KegbotDescriptor.getApiUrl(kegbotUrl));
    }
  }

  private void updateApiUrl(Uri apiUrl) {
    KegbotApi api = new KegbotApiImpl(new DefaultHttpClient(), apiUrl.toString());
    String username = PreferenceUtils.getUsername(mPreferences);
    String password = PreferenceUtils.getPassword(mPreferences);
    api.setAccountCredentials(username, password);
    mTapStatus.setKegbotApi(api);
    api = new KegbotApiImpl(new DefaultHttpClient(), apiUrl.toString());
    mEvents.setKegbotApi(api);
    loadUiFragments();
  }

  private void loadUiFragments() {
    mTapStatus.loadTap();
    mEvents.loadEvents();
  }
}
