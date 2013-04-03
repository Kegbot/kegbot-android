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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.event.TapListUpdateEvent;
import org.kegbot.app.service.CheckinService;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.KegTap;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gcm.GCMRegistrar;
import com.google.common.collect.Lists;
import com.squareup.otto.Subscribe;

public class HomeActivity extends CoreActivity {

  private static final String LOG_TAG = HomeActivity.class.getSimpleName();

  /**
   * Interval for periodic API polling.
   */
  private static final long REFRESH_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(30);

  private static final String GCM_SENDER_ID = "431392459978";

  private static final String ACTION_SHOW_TAP_EDITOR = "show_editor";
  private static final String EXTRA_METER_NAME = "meter_name";

  private HomeControlsFragment mControls;
  private SessionStatsFragment mSession;
  private EventListFragment mEvents;

  private KegbotCore mCore;

  private MyAdapter mTapStatusAdapter;
  private ViewPager mTapStatusPager;
  private AppConfiguration mConfig;
  private TapEditFragment mTapEditor;
  private final Handler mHandler = new Handler();

  /**
   * Shadow copy of tap manager taps. Updated, as needed, in
   * {@link #onTapListUpdate(TapListUpdateEvent)}.
   */
  private final List<KegTap> mTaps = Lists.newArrayList();

  private final Runnable mRefreshRunnable = new Runnable() {
    @Override
    public void run() {
      mEvents.loadEvents();
      mSession.loadCurrentSessionDetail();
      mHandler.postDelayed(this, REFRESH_INTERVAL_MILLIS);
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);

    mCore = KegbotCore.getInstance(this);
    mConfig = mCore.getConfiguration();

    mControls = new HomeControlsFragment();
    mEvents = new EventListFragment();
    mSession = new SessionStatsFragment();
    mTapEditor = new TapEditFragment();

    getFragmentManager().beginTransaction()
        .add(R.id.rightNav, mControls)
        .add(R.id.rightNav, mEvents)
        .add(R.id.rightNav, mSession)
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

    mTapStatusPager.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);

    // GCM
    GCMRegistrar.checkDevice(this);
    GCMRegistrar.checkManifest(this);
    final String regId = GCMRegistrar.getRegistrationId(this);
    if (regId.equals("")) {
      GCMRegistrar.register(this, GCM_SENDER_ID);
    } else {
      Log.v(LOG_TAG, "Already registered");
      mConfig.setGcmRegistrationId(regId);
    }
    // End GCM

    CheckinService.requestImmediateCheckin(this);

  }

  @Override
  protected void onResume() {
    Log.d(LOG_TAG, "onResume");
    super.onResume();
    mCore.getBus().register(this);
    handleIntent();
    startStatusPolling();
  }

  @Override
  protected void onPause() {
    Log.d(LOG_TAG, "--- unregistering");
    mCore.getBus().unregister(this);
    mHandler.removeCallbacks(mRefreshRunnable);
    super.onPause();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    switch (itemId) {
      case R.id.settings:
        SettingsActivity.startSettingsActivity(this);
        return true;
      case R.id.manageTap:
        showTapEditor(this, "");
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
    Log.d(LOG_TAG, "onNewIntent");
    super.onNewIntent(intent);
    setIntent(intent);
  }

  private void handleIntent() {
    final Intent intent = getIntent();
    Log.d(LOG_TAG, "Got intent: " + intent);

    if (ACTION_SHOW_TAP_EDITOR.equals(intent.getAction())) {
      String meterName = intent.getStringExtra(EXTRA_METER_NAME);
      if (getFragmentManager().getBackStackEntryCount() > 0) {
        Log.d(LOG_TAG, "Editor already showing.");
        return;
      }
      Log.d(LOG_TAG, "Showing tap editor for tap: " + meterName);
      final FragmentTransaction transaction = getFragmentManager().beginTransaction();
      transaction.remove(mControls);
      transaction.remove(mSession);
      transaction.remove(mEvents);
      transaction.add(R.id.rightNav, mTapEditor);
      transaction.addToBackStack("status");
      transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
      transaction.commit();
    }
  }

  private void startStatusPolling() {
    mHandler.removeCallbacks(mRefreshRunnable);
    mHandler.post(mRefreshRunnable);
  }

  @Subscribe
  public void onTapListUpdate(TapListUpdateEvent event) {
    Log.d(LOG_TAG, "Got tap list change event: " + event);

    final List<KegTap> newTapList = event.getTaps();
    if (newTapList.equals(mTaps)) {
      Log.d(LOG_TAG, "Tap list unchanged.");
      return;
    }

    mTaps.clear();
    mTaps.addAll(newTapList);
    if (mTapStatusPager.getCurrentItem() >= mTaps.size()) {
      if (!mTaps.isEmpty()) {
        mTapStatusPager.setCurrentItem(mTaps.size() - 1);
      }
    }
    mTapStatusAdapter.notifyDataSetChanged();
    if (!mTaps.isEmpty()) {
      setFocusedTap(mTaps.get(mTapStatusPager.getCurrentItem()));
    }
  }

  private void setFocusedTap(KegTap tap) {
    Log.d(LOG_TAG, "Set/replaced focused tap: " + tap.getMeterName());
    mCore.getTapManager().setFocusedTap(tap.getMeterName());
    mTapEditor.setTapDetail(tap);
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
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    startStatusPolling();
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
