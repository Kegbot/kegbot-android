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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.app.service.CheckinService;
import org.kegbot.app.util.PreferenceHelper;
import org.kegbot.core.AuthenticationManager;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.KegTap;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gcm.GCMRegistrar;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class HomeActivity extends CoreActivity {

  private static final String LOG_TAG = HomeActivity.class.getSimpleName();

  /**
   * Interval for periodic API polling.
   */
  private static final long REFRESH_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(30);

  private static final String GCM_SENDER_ID = "431392459978";

  private static final int REQUEST_AUTHENTICATE = RESULT_FIRST_USER + 1000;
  private static final int REQUEST_CREATE_DRINKER = RESULT_FIRST_USER + 1001;

  private EventListFragment mEvents;

  private KegbotApi mApi;

  private ViewGroup mControls;
  private Button mBeerMeButton;
  private Button mNewDrinkerButton;
  private KegbotCore mCore;

  private MyAdapter mTapStatusAdapter;
  private ViewPager mTapStatusPager;
  private SessionStatsFragment mSession;
  private PreferenceHelper mPrefsHelper;
  private final Handler mHandler = new Handler();

  private final List<KegTap> mTapDetails = Lists.newArrayList();
  private final Map<String, TapStatusFragment> mFragMap = Maps.newLinkedHashMap();

  private final Runnable mRefreshRunnable = new Runnable() {
    @Override
    public void run() {
      mEvents.loadEvents();
      mSession.loadCurrentSessionDetail();
      new TapLoaderTask().execute();
      mHandler.postDelayed(this, REFRESH_INTERVAL_MILLIS);
    }
  };

  private final OnClickListener mOnBeerMeClickedListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      final Intent intent = KegtabCommon.getAuthDrinkerActivityIntent(HomeActivity.this);
      startActivityForResult(intent, REQUEST_AUTHENTICATE);
    }
  };

  private final OnClickListener mOnNewDrinkerClickedListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      final Intent intent = KegtabCommon.getCreateDrinkerActivityIntent(HomeActivity.this);
      startActivityForResult(intent, REQUEST_CREATE_DRINKER);
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_activity);

    mCore = KegbotCore.getInstance(this);
    mApi = mCore.getApi();
    mPrefsHelper = mCore.getPreferences();

    mTapStatusAdapter = new MyAdapter(getFragmentManager());

    mTapStatusPager = (ViewPager) findViewById(R.id.tap_status_pager);
    mTapStatusPager.setAdapter(mTapStatusAdapter);
    mTapStatusPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        final TapStatusFragment frag = (TapStatusFragment) mTapStatusAdapter.getItem(position);
        final String meterName = frag.getTapDetail().getMeterName();

        Log.d(LOG_TAG, String.format("Selected tap: %s", meterName));
        mCore.getTapManager().setFocusedTap(frag.getTapDetail().getMeterName());
      }

      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      }

      @Override
      public void onPageScrollStateChanged(int state) {
      }
    });

    mEvents = (EventListFragment) getFragmentManager().findFragmentById(R.id.event_list);

    mSession = (SessionStatsFragment) getFragmentManager().findFragmentById(
        R.id.currentSessionFragment);

    mControls = (ViewGroup) findViewById(R.id.mainActivityControls);

    mBeerMeButton = (Button) findViewById(R.id.beerMeButton);
    mBeerMeButton.setOnClickListener(mOnBeerMeClickedListener);
    mNewDrinkerButton = (Button) findViewById(R.id.newDrinkerButton);
    mNewDrinkerButton.setOnClickListener(mOnNewDrinkerClickedListener);

    View v = findViewById(R.id.tap_status_pager);
    v.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);

    // GCM
    GCMRegistrar.checkDevice(this);
    GCMRegistrar.checkManifest(this);
    final String regId = GCMRegistrar.getRegistrationId(this);
    if (regId.equals("")) {
      GCMRegistrar.register(this, GCM_SENDER_ID);
    } else {
      Log.v(LOG_TAG, "Already registered");
      mPrefsHelper.setGcmRegistrationId(regId);
    }
    // End GCM

    CheckinService.requestImmediateCheckin(this);

  }

  @Override
  protected void onResume() {
    super.onResume();

    boolean showControls = false;

    if (mPrefsHelper.getAllowManualLogin()) {
      mBeerMeButton.setVisibility(View.VISIBLE);
      showControls = true;
    } else {
      mBeerMeButton.setVisibility(View.GONE);
    }

    if (mPrefsHelper.getAllowRegistration()) {
      mNewDrinkerButton.setVisibility(View.VISIBLE);
      showControls = true;
    } else {
      mNewDrinkerButton.setVisibility(View.GONE);
    }

    if (showControls && mPrefsHelper.getRunCore()) {
      mControls.setVisibility(View.VISIBLE);
    } else {
      mControls.setVisibility(View.GONE);
    }
    handleIntent();
    startStatusPolling();
  }

  @Override
  protected void onPause() {
    mHandler.removeCallbacks(mRefreshRunnable);
    super.onPause();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    if (itemId == R.id.settings) {
      SettingsActivity.startSettingsActivity(this);
      return true;
    } else if (itemId == R.id.bugreport) {
      BugreportActivity.startBugreportActivity(this);
      return true;
    } else if (itemId == android.R.id.home) {
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntent();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_AUTHENTICATE:
        Log.d(LOG_TAG, "Got authentication result.");
        if (resultCode == Activity.RESULT_OK && data != null) {
          final String username =
              data.getStringExtra(KegtabCommon.ACTIVITY_AUTH_DRINKER_RESULT_EXTRA_USERNAME);
          if (!Strings.isNullOrEmpty(username)) {
            Log.d(LOG_TAG, "Authenticating async.");
            AuthenticationManager am = mCore.getAuthenticationManager();
            am.authenticateUsernameAsync(username);
          }
        }
        break;
      case REQUEST_CREATE_DRINKER:
        Log.d(LOG_TAG, "Got registration result.");
        if (resultCode == Activity.RESULT_OK && data != null) {
          final String username =
              data.getStringExtra(KegtabCommon.ACTIVITY_CREATE_DRINKER_RESULT_EXTRA_USERNAME);
          if (!Strings.isNullOrEmpty(username)) {
            Log.d(LOG_TAG, "Authenticating newly-created user.");
            AuthenticationManager am = mCore.getAuthenticationManager();
            am.authenticateUsernameAsync(username);
          }
        }
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void handleIntent() {
    final Intent intent = getIntent();
    Log.d(LOG_TAG, "Got intent: " + intent);
  }

  private void startStatusPolling() {
    mHandler.removeCallbacks(mRefreshRunnable);
    mHandler.post(mRefreshRunnable);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    startStatusPolling();
  }

  public class MyAdapter extends FragmentPagerAdapter {
    public MyAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int arg0) {
      KegTap tap = mTapDetails.get(arg0);
      return getTapFragment(tap);
    }

    @Override
    public int getCount() {
      return mTapDetails.size();
    }

  }

  private TapStatusFragment getTapFragment(KegTap tap) {
    final String tapName = tap.getMeterName();
    if (!mFragMap.containsKey(tapName)) {
      final TapStatusFragment frag = new TapStatusFragment();
      frag.setTapDetail(tap);
      mFragMap.put(tapName, frag);
    }
    return mFragMap.get(tapName);
  }

  private class TapLoaderTask extends AsyncTask<Void, Void, List<KegTap>> {

    @Override
    protected List<KegTap> doInBackground(Void... params) {
      try {
        return mApi.getAllTaps();
      } catch (KegbotApiException e) {
        Log.e(LOG_TAG, "Api error.", e);
        return null;
      }
    }

    @Override
    protected void onPostExecute(List<KegTap> result) {
      if (result == null) {
        return;
      }
      try {
        for (final KegTap tap : result) {
          final String tapName = tap.getMeterName();
          if (!mFragMap.containsKey(tapName)) {
            Log.d(LOG_TAG, "Adding new tap: " + tapName);
            Log.d(LOG_TAG, "Tap details: " + tap);
            mTapDetails.add(tap);
            mTapStatusAdapter.notifyDataSetChanged();
          } else {
            Log.d(LOG_TAG, "Updating tap: " + tapName);
            mFragMap.get(tapName).setTapDetail(tap);
          }
        }
      } catch (Throwable e) {
        Log.wtf("TapStatusFragment", "UNCAUGHT EXCEPTION", e);
      }
    }
  }

}
