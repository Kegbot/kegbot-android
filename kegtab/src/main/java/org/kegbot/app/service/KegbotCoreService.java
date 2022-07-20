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
package org.kegbot.app.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.kegbot.app.HomeActivity;
import org.kegbot.app.PourInProgressActivity;
import org.kegbot.app.R;
import org.kegbot.app.alert.AlertCore;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.event.ConnectivityChangedEvent;
import org.kegbot.app.event.FlowUpdateEvent;
import org.kegbot.core.Flow;
import org.kegbot.core.FlowManager;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.SyncManager;
import org.kegbot.core.hardware.HardwareManager;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Primary service for running this kegbot.
 */
public class KegbotCoreService extends Service {

  private final static String TAG = KegbotCoreService.class.getSimpleName();

  private static final long ACTIVITY_START_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(2);
  private long mLastPourActivityStart = 0;

  private KegbotCore mCore;
  private FlowManager mFlowManager;
  private AppConfiguration mConfig;

  private HardwareManager mHardwareManager;
  private SyncManager mApiManager;
  private PowerManager.WakeLock mWakeLock;

  private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

  private final IntentFilter mIntentFilter =
      new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
  private BroadcastReceiver mBroadcastReceiver;

  private final FlowManager.Listener mFlowListener = new FlowManager.Listener() {
    @Override
    public void onFlowStart(final Flow flow) {
      Log.d(TAG, "onFlowStart: " + flow);
      if (flow.getTap() == null) {
        Log.d(TAG, "Unbound flow; no updated will be posted.");
        mCore.getAlertCore().postAlert(
            AlertCore.newBuilder(getString(R.string.alert_flow_activity_title))
                .setDescription(getString(R.string.alert_flow_activity_description, flow.getMeterName()))
                .severityError()
                .setId(flow.getMeterName())
                .build()
        );
        return;
      }

      if (flow.isAuthenticated()) {
        mHardwareManager.toggleOutput(flow.getTap(), true);
      }
      startPourActivity();
      mCore.postEvent(new FlowUpdateEvent(flow));
    }

    @Override
    public void onFlowUpdate(final Flow flow) {
      if (flow.getTap() == null) {
        return;
      }
      if (flow.isAuthenticated()) {
        mHardwareManager.toggleOutput(flow.getTap(), true);
      }
      mCore.postEvent(new FlowUpdateEvent(flow));
    }

    @Override
    public void onFlowEnd(final Flow flow) {
      Log.d(TAG, "onFlowEnd" + flow);
      if (flow.getTap() == null) {
        return;
      }
      mHardwareManager.toggleOutput(flow.getTap(), false);
      final Runnable r = () -> {
        Log.d(TAG, "Flow ended: " + flow);
        recordDrinkForFlow(flow);
      };
      mExecutorService.submit(r);
    }
  };

  private void startPourActivity() {
    Log.d(TAG, "startPourActivity");
    long now = SystemClock.elapsedRealtime();
    if ((now - mLastPourActivityStart) > ACTIVITY_START_TIMEOUT_MILLIS) {
      mLastPourActivityStart = now;
      final Intent intent = PourInProgressActivity.getStartIntent(this);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      Log.d(TAG, "Starting pour activity");
      startActivity(intent);
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate()");
    mCore = KegbotCore.getInstance(this);
    mFlowManager = mCore.getFlowManager();
    mApiManager = mCore.getSyncManager();
    mHardwareManager = mCore.getHardwareManager();
    mConfig = mCore.getConfiguration();

    // TODO: this should be part of a config update event.
    mCore.getImageDownloader().setBaseUrl(mConfig.getKegbotUrl());

    mFlowManager.addFlowListener(mFlowListener);

    updateFromPreferences();

    mCore.start();

    mBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        // TODO(mikey): Refactor with similar method in syncManager.
        final ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        final boolean connected;
        final String message;
        if (activeNetwork != null && activeNetwork.isConnected()) {
          connected = true;
          message = String.format("Connected to %s", activeNetwork.getTypeName());
        } else {
          connected = false;
          message = "Network not connected.";
        }
        mCore.postEvent(new ConnectivityChangedEvent(connected, message));
      }
    };
    registerReceiver(mBroadcastReceiver, mIntentFilter);


    if (mConfig.stayAwake()) {
      // CoreActivity will keep the screen on when a Kegbot activity is
      // in the foreground; we only need to worry about holding a partial
      // wakelock.
      PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
      mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kegbot:kegbot-core");
      mWakeLock.acquire();
    } else {
      mWakeLock = null;
    }
  }

  /**
   * Binder interface to this service. Local binds only.
   */
  public class LocalBinder extends Binder {
    public KegbotCoreService getService() {
      return KegbotCoreService.this;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null; // Not bindable.
  }

  @Override
  protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    mCore.dump(writer);
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy()");
    unregisterReceiver(mBroadcastReceiver);

    mFlowManager.removeFlowListener(mFlowListener);
    mCore.stop();
    if (mWakeLock != null) {
      mWakeLock.release();
      mWakeLock = null;
    }

    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_STICKY;
  }

  public static void startService(Context context) {
    final Intent intent = new Intent(context, KegbotCoreService.class);
    context.startService(intent);
  }

  public static void stopService(Context context) {
    final Intent intent = new Intent(context, KegbotCoreService.class);
    context.stopService(intent);
  }

  private void updateFromPreferences() {
    final boolean runCore = mConfig.getRunCore();
    if (runCore) {
      debugNotice("Running core!");
      mFlowManager.addFlowListener(mFlowListener);
      ContextCompat.startForegroundService(this, buildForegroundServiceIntent());
    } else {
      debugNotice("Stopping core.");
      mFlowManager.removeFlowListener(mFlowListener);
      mCore.stop();
      stopForeground(true);
    }
  }

  private Intent buildForegroundServiceIntent() {
    final Intent intent = new Intent(this, HomeActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    return intent;
  }

  private void recordDrinkForFlow(final Flow ended) {
    long minVolume = mConfig.getMinimumVolumeMl();
    if (ended.getVolumeMl() < minVolume) {
      Log.i(TAG, "Not recording flow: "
          + "volume (" + ended.getVolumeMl() + " mL) is less than minimum "
          + "(" + minVolume + " mL)");
      return;
    }
    Log.d(TAG, "Recording drink for flow: " + ended);
    Log.d(TAG, "Tap: " + ended.getTap());
    mApiManager.recordDrinkAsync(ended);
  }

  private void debugNotice(String message) {
    Log.d(TAG, message);
  }

}
