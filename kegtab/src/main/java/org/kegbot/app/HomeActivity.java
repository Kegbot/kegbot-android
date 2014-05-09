/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.hoho.android.usbserial.util.HexDump;
import com.squareup.otto.Subscribe;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.event.ConnectivityChangedEvent;
import org.kegbot.app.event.VisibleTapsChangedEvent;
import org.kegbot.app.service.CheckinService;
import org.kegbot.app.util.Utils;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.KegTap;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * The main "home screen" of the Kegtab application. It shows the status of each tap, and allows the
 * user start a pour by authenticating (if enabled in settings).
 */
public class HomeActivity extends CoreActivity {

  private static final String LOG_TAG = HomeActivity.class.getSimpleName();

  //private static final int REQUEST_PLAY_SERVICES_UPDATE = 100;
  private static final String GCM_SENDER_ID = "209039242857";

  private static final String ACTION_SHOW_TAP_EDITOR = "show_editor";
  private static final String EXTRA_METER_NAME = "meter_name";

  /**
   * Idle timeout which triggers "attract mode".
   *
   * @see #mAttractModeRunnable
   */
  private static final long IDLE_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(10);

  /**
   * Pause interval between rotated screens in "attract mode".
   *
   * @see #mAttractModeRunnable
   */
  private static final long ROTATE_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(12);

  private HomeControlsFragment mControls;
  private SessionStatsFragment mSession;
  private EventListFragment mEvents;

  private KegbotCore mCore;

  private MyAdapter mTapStatusAdapter;
  private ViewPager mTapStatusPager;
  private AppConfiguration mConfig;

  /**
   * Keep track of Google Play Services error codes, and don't annoy when the same error persists.
   * (For some reason, {@link GooglePlayServicesUtil} treats absence of the apk as "user
   * recoverable").
   *
   * @see #checkPlayServices()
   */
  private int mLastShownGooglePlayServicesError = Integer.MIN_VALUE;

  /**
   * Shadow copy of tap manager taps.
   */
  private final List<KegTap> mTaps = Lists.newArrayList();

  /** Main thread handler for managing {@link #mAttractModeRunnable}. */
  private final Handler mAttractModeHandler = new Handler(Looper.getMainLooper());

  /**
   * Rotates through view pager when idle.
   *
   * @see #startAttractMode()
   * @see #resetAttractMode()
   * @see #cancelAttractMode()
   */
  private final Runnable mAttractModeRunnable = new Runnable() {
    @Override
    public void run() {
      rotateDisplay();
      mAttractModeHandler.postDelayed(mAttractModeRunnable, ROTATE_INTERVAL_MILLIS);
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);

    mControls = new HomeControlsFragment();
    mEvents = new EventListFragment();
    mSession = new SessionStatsFragment();

    getFragmentManager().beginTransaction()
        .add(R.id.rightNav, mControls)
        .add(R.id.rightNav, mSession)
        .add(R.id.rightNav, mEvents)
        .disallowAddToBackStack()
        .setTransition(FragmentTransaction.TRANSIT_NONE)
        .commit();

    mTapStatusAdapter = new MyAdapter(getFragmentManager());

    mTapStatusPager = (ViewPager) findViewById(R.id.tap_status_pager);
    mTapStatusPager.setAdapter(mTapStatusAdapter);
    mTapStatusPager.setOffscreenPageLimit(8); // >8 Tap systems are rare
    mTapStatusPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        final TapStatusFragment frag = (TapStatusFragment) mTapStatusAdapter.getItem(position);
        setFocusedTap(frag.getTapDetail());
      }

      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      }

      @Override
      public void onPageScrollStateChanged(int state) {
      }
    });

    CheckinService.requestImmediateCheckin(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    Log.d(LOG_TAG, "onResume");
    super.onResume();
    mCore = KegbotCore.getInstance(this);
    mConfig = mCore.getConfiguration();
    mCore.getBus().register(this);
    mCore.getHardwareManager().refreshSoon();
    startAttractMode();

    if (checkPlayServices()) {
      doGcmRegistration();
    }
  }

  @Override
  protected void onPause() {
    Log.d(LOG_TAG, "--- unregistering");
    mCore.getBus().unregister(this);
    cancelAttractMode();
    super.onPause();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    switch (itemId) {
      case R.id.settings:
        SettingsActivity.startSettingsActivity(this);
        return true;
      case R.id.manageTaps:
        TapListActivity.startActivity(this);
        return true;
      case R.id.bugreport:
        BugreportActivity.startBugreportActivity(this);
        return true;
      case android.R.id.home:
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    Log.d(LOG_TAG, "onNewIntent: Got intent: " + intent);

    if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
      Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
      byte[] id = tag.getId();
      if (id != null && id.length > 0) {
        String tagId = HexDump.toHexString(id).toLowerCase(Locale.US);
        Log.d(LOG_TAG, "Read NFC tag with id: " + tagId);
        // TODO: use tag technology as part of id?
        AuthenticatingActivity.startAndAuthenticate(this, "nfc", tagId);
      }
    }
  }

  @Subscribe
  public void onVisibleTapListUpdate(VisibleTapsChangedEvent event) {
    Log.d(LOG_TAG, "Got tap list change event: " + event);

    final List<KegTap> newTapList = event.getTaps();
    if (newTapList.equals(mTaps)) {
      Log.d(LOG_TAG, "Tap list unchanged.");
      return;
    }

    mTaps.clear();
    mTaps.addAll(newTapList);
    mTapStatusAdapter.notifyDataSetChanged();

    if (mTapStatusPager.getCurrentItem() >= mTaps.size()) {
      if (!mTaps.isEmpty()) {
        mTapStatusPager.setCurrentItem(mTaps.size() - 1);
      }
    }

    if (!mTaps.isEmpty()) {
      setFocusedTap(mTaps.get(mTapStatusPager.getCurrentItem()));
    }
  }

  @Subscribe
  public void onConnectivityChangedEvent(ConnectivityChangedEvent event) {
    updateConnectivityAlert(event.isConnected());
  }

  private void setFocusedTap(KegTap tap) {
    Log.d(LOG_TAG, "Set/replaced focused tap: " + tap.getId());
  }

  /**
   * Shows the tap editor for the given tap, prompting for the manager pin if necessary.
   *
   * @param context
   * @param meterName
   */
  static void showTapEditor(Context context, String meterName) {
    final Intent editorIntent = new Intent(context, HomeActivity.class);
    editorIntent.setAction(ACTION_SHOW_TAP_EDITOR);
    editorIntent.putExtra(EXTRA_METER_NAME, meterName);
    editorIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    PinActivity.startThroughPinActivity(context, editorIntent);
  }

  @Override
  public void onUserInteraction() {
    resetAttractMode();
  }

  private void rotateDisplay() {
    final int count = mTapStatusAdapter.getCount();
    if (count <= 1) {
      return;
    }
    final int nextItem = (mTapStatusPager.getCurrentItem() + 1) % mTapStatusAdapter.getCount();
    mTapStatusPager.setCurrentItem(nextItem);
  }

  private void startAttractMode() {
    cancelAttractMode();
    if (mConfig.getEnableAttractMode()) {
      mAttractModeHandler.postDelayed(mAttractModeRunnable, IDLE_TIMEOUT_MILLIS);
    }
  }

  private void resetAttractMode() {
    cancelAttractMode();
    startAttractMode();
  }

  private void cancelAttractMode() {
    mAttractModeHandler.removeCallbacks(mAttractModeRunnable);
  }

  private void doGcmRegistration() {
    final int versionCode = Utils.getOwnPackageInfo(getApplicationContext()).versionCode;
    final int registeredVersionCode = mConfig.getGcmRegistrationAppVersion();
    final String currentRegId = mConfig.getGcmRegistrationId();

    // Fast path: reuse saved id.
    if (versionCode == registeredVersionCode && !Strings.isNullOrEmpty(currentRegId)) {
      return;
    }

    // Destroy stale regid, if any.
    mConfig.setGcmRegistrationId("");

    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        Log.d(LOG_TAG, "Registering for GCM ...");
        final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(HomeActivity.this);
        final String gcmId;
        try {
          gcmId = gcm.register(GCM_SENDER_ID);
        } catch (IOException e) {
          Log.w(LOG_TAG, "GCM registration failed.", e);
          return null;
        }
        mConfig.setGcmRegistrationId(gcmId);
        mConfig.setGcmRegistrationAppVersion(versionCode);
        CheckinService.requestImmediateCheckin(getApplicationContext());
        Log.d(LOG_TAG, "GCM registration success, id=" + gcmId);

        return null;
      }
    }.execute(null, null, null);
  }

  private boolean checkPlayServices() {
    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (resultCode != ConnectionResult.SUCCESS) {
      if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
        Log.i(LOG_TAG, "GCM error: " + resultCode);
        if (resultCode != mLastShownGooglePlayServicesError) {
          Log.w(LOG_TAG, GooglePlayServicesUtil.getErrorString(resultCode));
          //GooglePlayServicesUtil.getErrorDialog(
          //    resultCode, this, REQUEST_PLAY_SERVICES_UPDATE).show();
          mLastShownGooglePlayServicesError = resultCode;
        }
      }
      return false;
    }
    return true;
  }

  public class MyAdapter extends FragmentStatePagerAdapter {
    public MyAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int index) {
      if (index >= mTaps.size()) {
        Log.wtf(LOG_TAG, "Trying to get fragment " + index + ", current size " + mTaps.size());
      }
      TapStatusFragment frag = new TapStatusFragment();
      frag.setTapDetail(mTaps.get(index));
      return frag;
    }

    @Override
    public int getItemPosition(Object object) {
      Log.d(LOG_TAG, "getItemPosition: " + object);
      return POSITION_NONE;
    }

    @Override
    public int getCount() {
      return mTaps.size();
    }
  }

}
