/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.squareup.otto.Subscribe;

import org.apache.commons.codec.binary.Hex;
import org.kegbot.app.alert.AlertCore;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.event.ConnectivityChangedEvent;
import org.kegbot.app.event.VisibleTapsChangedEvent;
import org.kegbot.app.util.SortableFragmentStatePagerAdapter;
import org.kegbot.app.util.Utils;
import org.kegbot.core.KegbotCore;
import org.kegbot.proto.Models.KegTap;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

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

  private static final String ALERT_ID_UNBOUND_TAPS = "unbound-taps";

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

  private static final Function<KegTap, String> TAP_TO_NAME = new Function<KegTap, String>() {
    @Nullable
    @Override
    public String apply(@Nullable KegTap input) {
      return input != null ? input.getName() : "none";
    }
  };

  private KegbotCore mCore;

  private HomeFragmentsAdapter mTapStatusAdapter;
  private ViewPager mTapStatusPager;
  private AppConfiguration mConfig;

  private final Object mTapsLock = new Object();

  /**
   * Shadow copy of tap manager taps.
   */
  @GuardedBy("mTapsLock")
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

    synchronized (mTapsLock) {
      mTaps.clear();
    }
    mTapStatusAdapter = new HomeFragmentsAdapter(getFragmentManager());

    mTapStatusPager = (ViewPager) findViewById(R.id.tap_status_pager);
    mTapStatusPager.setAdapter(mTapStatusAdapter);
    mTapStatusPager.setOffscreenPageLimit(8); // >8 Tap systems are rare
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mCore = KegbotCore.getInstance(this);
    mConfig = mCore.getConfiguration();
    maybeShowTapWarnings();
  }

  @Override
  protected void onResume() {
    Log.d(LOG_TAG, "onResume");
    super.onResume();
    mCore.getBus().register(this);
    mCore.getHardwareManager().refreshSoon();
    startAttractMode();
  }

  @Override
  protected void onPause() {
    Log.d(LOG_TAG, "onPause");
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
        String tagId = Hex.encodeHexString(id).toLowerCase();
        Log.d(LOG_TAG, "Read NFC tag with id: " + tagId);
        // TODO: use tag technology as part of id?
        AuthenticatingActivity.startAndAuthenticate(this, "nfc", tagId);
      }
    }
  }

  @Subscribe
  public void onVisibleTapListUpdate(VisibleTapsChangedEvent event) {
    assert(Looper.myLooper() == Looper.getMainLooper());
    Log.d(LOG_TAG, "Got tap list change event: " + event + " taps=" + event.getTaps().size());

    final List<KegTap> newTapList = event.getTaps();
    synchronized (mTapsLock) {
      if (newTapList.equals(mTaps)) {
        Log.d(LOG_TAG, "Tap list unchanged.");
        return;
      }

      mTaps.clear();
      mTaps.addAll(newTapList);
      mTapStatusAdapter.notifyDataSetChanged();
    }

    maybeShowTapWarnings();
  }

  private void maybeShowTapWarnings() {
    final List<KegTap> unboundTaps = Lists.newArrayList();
    synchronized (mTapsLock) {
      for (final KegTap tap : mTaps) {
        if (!tap.hasMeter()) {
          unboundTaps.add(tap);
        }
      }
    }

    if (unboundTaps.isEmpty()) {
      mCore.getAlertCore().cancelAlert(ALERT_ID_UNBOUND_TAPS);
      return;
    }

    final String message;
    final List<String> tapNames = Lists.transform(unboundTaps, TAP_TO_NAME);
    if (tapNames.size() == 1) {
      message = getString(R.string.alert_unbound_single_tap_description,
          tapNames.get(0));
    } else {
      final String listStr = Joiner.on(", ").join(tapNames.subList(0, tapNames.size() - 1));
      message = getString(R.string.alert_unbound_multiple_taps_description, listStr,
          tapNames.get(tapNames.size() - 1));
    }

    mCore.getAlertCore().postAlert(AlertCore.newBuilder(getString(R.string.alert_unbound_title))
        .setId(ALERT_ID_UNBOUND_TAPS)
        .setAction(new Runnable() {
          @Override
          public void run() {
            TapListActivity.startActivity(getApplicationContext());
          }
        })
        .setActionName(getString(R.string.alert_unbound_action_name))
        .setDescription(message)
        .severityWarning()
        .build());
  }

  @Subscribe
  public void onConnectivityChangedEvent(ConnectivityChangedEvent event) {
    updateConnectivityAlert(event.isConnected());
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

  /**
   * Shows a TapStatusFragment for each tap, plus a SystemStatusFragment.
   */
  public class HomeFragmentsAdapter extends SortableFragmentStatePagerAdapter {
    public HomeFragmentsAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public long getItemId(int position) {
      if (position < mTaps.size()) {
        return mTaps.get(position).getId();
      } else if (position == mTaps.size()) {
        return -1;
      }
      throw new IndexOutOfBoundsException("Position out of bounds: " + position);
    }

    @Override
    public Fragment getItem(int index) {
      Log.d(LOG_TAG, "getItem: " + index);
      synchronized (mTapsLock) {
        if (index < mTaps.size()) {
          final KegTap tap = mTaps.get(index);
          TapStatusFragment frag = TapStatusFragment.forTap(mTaps.get(index));
          return frag;
        } else if (index == mTaps.size()) {
          SystemStatusFragment frag = new SystemStatusFragment();
          return frag;
        } else {
          Log.wtf(LOG_TAG, "Trying to get fragment " + index + ", current size " + mTaps.size());
          return null;
        }
      }
    }

    @Override
    public int getItemPosition(Object object) {
      Log.d(LOG_TAG, "getItemPosition: " + object);

      synchronized (mTapsLock) {
        if (object instanceof SystemStatusFragment) {
          Log.d(LOG_TAG, "  position=" + mTaps.size());
          return mTaps.size();
        }

        if (object instanceof TapStatusFragment) {
          final int tapId = ((TapStatusFragment) object).getTapId();
          int position = 0;
          for (final KegTap tap : mTaps) {
            if (tap.getId() == tapId) {
              Log.d(LOG_TAG, "  position=" + position);
              return position;
            }
            position++;
          }
        }
      }

      Log.d(LOG_TAG, "  position=NONE");
      return POSITION_NONE;
    }

    @Override
    public int getCount() {
      synchronized (mTapsLock) {
        return mTaps.size() + 1;
      }
    }

    @Override
    public float getPageWidth(int position) {
      return 0.5f;
    }
  }

}
