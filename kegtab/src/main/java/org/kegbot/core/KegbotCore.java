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
package org.kegbot.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.AuthenticatingActivity;
import org.kegbot.app.KegbotApplication;
import org.kegbot.app.NewControllerActivity;
import org.kegbot.app.alert.AlertCore;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.config.SharedPreferencesConfigurationStore;
import org.kegbot.app.util.DeviceId;
import org.kegbot.app.util.ImageDownloader;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.app.util.Utils;
import org.kegbot.backend.Backend;
import org.kegbot.backend.BackendException;
import org.kegbot.backend.LocalBackend;
import org.kegbot.core.FlowManager.Clock;
import org.kegbot.core.hardware.Controller;
import org.kegbot.core.hardware.ControllerAttachedEvent;
import org.kegbot.core.hardware.HardwareManager;
import org.kegbot.core.hardware.ThermoSensorUpdateEvent;
import org.kegbot.core.hardware.TokenAttachedEvent;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Models;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Top-level class implementing the Kegbot core.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class KegbotCore {

  private static final String TAG = KegbotCore.class.getSimpleName();

  private static KegbotCore sInstance;

  private final Bus mBus;
  private final BusListener mBusListener = new BusListener();
  private final Handler mBusHandler = new Handler(Looper.getMainLooper());

  private final AppConfiguration mConfig;
  private final SharedPreferences mSharedPreferences;
  private ExecutorService mExecutorService;

  private final Set<Manager> mManagers = Sets.newLinkedHashSet();
  private final TapManager mTapManager;
  private final FlowManager mFlowManager;
  private final AuthenticationManager mAuthenticationManager;
  private final SoundManager mSoundManager;
  private final ImageDownloader mImageDownloader;
  private final AlertCore mAlertCore;

  private final Backend mBackend;
  private final SyncManager mSyncManager;

  private final HardwareManager mHardwareManager;

  private final BluetoothManager mBluetoothManager;

  private final Context mContext;

  private boolean mStarted = false;

  private final FlowManager.Clock mClock = new Clock() {
    @Override
    public long elapsedRealtime() {
      return System.currentTimeMillis();
    }
  };

  private KegbotCore(Context context) {
    mContext = context.getApplicationContext();
    mBus = new Bus(ThreadEnforcer.MAIN);

    mConfig = ((KegbotApplication) mContext.getApplicationContext()).getConfig();
    mSharedPreferences = ((KegbotApplication) mContext.getApplicationContext()).getSharedPreferences();

    if (mConfig.isLocalBackend()) {
      Log.d(TAG, "Using local backend.");
      mBackend = new LocalBackend();
    } else {
      Log.d(TAG, "Using server backend.");
      mBackend = new KegbotApiImpl(mConfig);
    }

    mImageDownloader = new ImageDownloader(context, mConfig.getKegbotUrl());

    mTapManager = new TapManager(mBus,
        SharedPreferencesConfigurationStore.fromName(mContext, "tap_manager_prefs"));
    mManagers.add(mTapManager);

    mFlowManager = new FlowManager(mBus, mTapManager, mConfig, mClock);
    mManagers.add(mFlowManager);

    mSyncManager = new SyncManager(mBus, context, mBackend, mTapManager);
    mManagers.add(mSyncManager);

    mHardwareManager = new HardwareManager(mBus, context, mConfig, mBackend);
    mManagers.add(mHardwareManager);

    mAuthenticationManager = new AuthenticationManager(mBus, context, mBackend, mConfig);
    mManagers.add(mAuthenticationManager);

    mSoundManager = new SoundManager(mBus, context);
    mManagers.add(mSoundManager);

    mBluetoothManager = new BluetoothManager(mBus, context);
    mManagers.add(mBluetoothManager);

    mAlertCore = new AlertCore(mBus, mContext);
    mManagers.add(mAlertCore);
  }

  public synchronized void start() {
    Log.d(TAG, "start");
    if (!mStarted) {
      mBus.register(mBusListener);
      Log.i(TAG, "Starting up, backend:" + mBackend);
      mExecutorService = Executors.newSingleThreadExecutor();
      mBackend.start(mContext);
      for (final Manager manager : mManagers) {
        Log.d(TAG, "Starting " + manager.getName());
        manager.start();
      }
      mStarted = true;
    }
  }

  public synchronized void stop() {
    Log.d(TAG, "stop");
    if (mStarted) {
      mBus.unregister(mBusListener);
      mExecutorService.shutdown();
      mExecutorService = null;
      for (final Manager manager : mManagers) {
        Log.d(TAG, "Stopping " + manager.getName());
        manager.stop();
      }
      mStarted = false;
    }
    sInstance = null;
  }

  public Bus getBus() {
    return mBus;
  }

  /**
   * Posts event on the main (UI) thread.
   *
   * @param event the event
   */
  public void postEvent(final Object event) {
    mBusHandler.post(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Posting event: " + event);
        mBus.post(event);
      }
    });
  }

  /**
   * @return the preferences
   */
  public AppConfiguration getConfiguration() {
    return mConfig;
  }

  /**
   * @return the tapManager
   */
  public TapManager getTapManager() {
    return mTapManager;
  }

  /**
   * @return the authenticationManager
   */
  public AuthenticationManager getAuthenticationManager() {
    return mAuthenticationManager;
  }

  /**
   * @return the backend
   */
  public Backend getBackend() {
    return mBackend;
  }

  /**
   * @return the sync manager
   */
  public SyncManager getSyncManager() {
    return mSyncManager;
  }

  /**
   * @return the flowManager
   */
  public FlowManager getFlowManager() {
    return mFlowManager;
  }

  /**
   * @return the soundManager
   */
  public SoundManager getSoundManager() {
    return mSoundManager;
  }

  /**
   * @return the hardwareManager
   */
  public HardwareManager getHardwareManager() {
    return mHardwareManager;
  }

  public ImageDownloader getImageDownloader() {
    return mImageDownloader;
  }

  public AlertCore getAlertCore() {
    return mAlertCore;
  }

  public synchronized String getDeviceId() {
    String id = mSharedPreferences.getString("device_id", "");
    if (Strings.isNullOrEmpty(id)) {
      id = DeviceId.getDeviceId(mContext);
      setDeviceId(id);
    }
    return id;
  }

  public synchronized void setDeviceId(String deviceId) {
    mSharedPreferences.edit().putString("device_id", deviceId).apply();
  }

  public void dump(PrintWriter printWriter) {
    StringWriter baseWriter = new StringWriter();
    IndentingPrintWriter writer = new IndentingPrintWriter(baseWriter, "  ");

    writer.println("## System info");
    writer.increaseIndent();
    writer.printPair("userAgent", System.getProperty("http.agent")).println();
    writer.printPair("fingerprint", Build.FINGERPRINT).println();
    writer.printPair("board", Build.BOARD).println();
    writer.printPair("device", Build.DEVICE).println();
    writer.printPair("model", Build.MODEL).println();
    writer.printPair("manufacturer", Build.MANUFACTURER).println();
    writer.printPair("sdk", Integer.valueOf(Build.VERSION.SDK_INT)).println();
    writer.decreaseIndent();
    writer.println();

    writer.println("## Prefs");
    writer.increaseIndent();
    for (Map.Entry<String, ?> entry : mSharedPreferences.getAll().entrySet()) {
      final String key = entry.getKey();
      String value = String.valueOf(entry.getValue());

      if (key.equals("config:PIN") || key.equals("config:API_KEY")) {
        if (value.isEmpty()) {
          value = "(unset)";
        } else {
          value = "(redacted)";
        }
      }

      writer.printPair(key, value).println();
    }
    writer.println();
    writer.decreaseIndent();

    try {
      final PackageInfo packageInfo = Utils.getOwnPackageInfo(mContext);

      writer.println("## Package info");
      writer.println();
      writer.increaseIndent();

      writer.printPair("releaseBuild",
          Boolean.valueOf(((KegbotApplication) mContext.getApplicationContext()).isReleaseBuild()))
          .println();
      if (packageInfo != null) {
        writer.printPair("versionName", packageInfo.versionName).println();
        writer.printPair("versionCode", String.valueOf(packageInfo.versionCode)).println();
        writer.printPair("packageName", packageInfo.packageName).println();
        writer.printPair("installTime", new Date(packageInfo.firstInstallTime)).println();
        writer.printPair("lastUpdateTime", new Date(packageInfo.lastUpdateTime)).println();
        if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
          writer.printPair("signature", Utils.getFingerprintForSignature(packageInfo.signatures[0]))
              .println();
        }
      }

      final PackageManager pm = mContext.getPackageManager();
      writer.printPair("installerPackageName", pm.getInstallerPackageName(mContext.getPackageName()))
          .println();

      writer.decreaseIndent();
      writer.println();

      writer.println("## Core info");
      writer.println();
      writer.increaseIndent();
      writer.printPair("mStarted", Boolean.valueOf(mStarted)).println();
      writer.printPair("deviceId", getDeviceId()).println();
      writer.printPair("gcmId", mConfig.getGcmRegistrationId()).println();
      writer.printPair("enableFlowAutoStart", Boolean.valueOf(mConfig.getEnableFlowAutoStart()))
          .println();
      writer.printPair("allowManualLogin", Boolean.valueOf(mConfig.getAllowManualLogin()))
          .println();
      writer.printPair("allowRegistration", Boolean.valueOf(mConfig.getAllowRegistration()))
          .println();
      writer.printPair("cacheCredentials", Boolean.valueOf(mConfig.getCacheCredentials()))
          .println();
      writer.println();

      for (final Manager manager : mManagers) {
        writer.println(String.format("## %s", manager.getName()));
        writer.increaseIndent();
        manager.dump(writer);
        writer.decreaseIndent();
        writer.println();
      }
      writer.decreaseIndent();
    } finally {
      writer.close();
    }

    writer.println("## Threads:");
    writer.increaseIndent();
    for (final Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
      final Thread thr = entry.getKey();
      final StackTraceElement[] elements = entry.getValue();

      writer.println(thr.toString());
      writer.increaseIndent();
      for (final StackTraceElement elem : elements) {
        writer.println(elem);
      }
      writer.decreaseIndent();
      writer.println();
    }

    printWriter.write(baseWriter.toString());
  }

  public static KegbotCore getInstance(Context context) {
    synchronized (KegbotCore.class) {
      if (sInstance == null) {
        sInstance = new KegbotCore(context.getApplicationContext());
      }
    }
    return sInstance;
  }

  /** Like {@link #getInstance(Context)}, but does not create the instance. */
  public static KegbotCore getRunningInstance(Context context) {
    synchronized (KegbotCore.class) {
      return sInstance;
    }
  }

  private class BusListener {

    @Subscribe
    public void onTokenAdded(TokenAttachedEvent event) {
      final AuthenticationToken token = event.getToken();
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "onTokenAttached: running");
          AuthenticatingActivity.startAndAuthenticate(mContext,
              token.getAuthDevice(), token.getTokenValue());
        }
      };
      mBusHandler.post(r);
    }

    @Subscribe
    public void onThermoSensorUpdate(final ThermoSensorUpdateEvent event) {
      final ThermoSensor sensor = event.getSensor();
      Log.d(TAG, "Sensor update for sensor: " + sensor);
      final RecordTemperatureRequest request = RecordTemperatureRequest.newBuilder()
          .setSensorName(sensor.getName()).setTempC((float) sensor.getTemperatureC())
          .buildPartial();
      mSyncManager.recordTemperatureAsync(request);
    }

    @Subscribe
    public void onControllerAttached(ControllerAttachedEvent event) {
      final Controller controller = event.getController();
      Log.d(TAG, "Controller attached: " + controller + " status='" + controller.getStatus() + "'");

      final String alertId = controller.getName();
      getAlertCore().cancelAlert(alertId);

      if (controller.getStatus().equals(Controller.STATUS_NAME_CONFLICT)) {
        final AlertCore.Alert alert = AlertCore.newBuilder("Controller conflict")
            .setDescription("Multiple controllers named " + controller.getName() +
                " are attached.  Only the first controller is enabled.")
            .severityError()
            .setId(alertId)
            .build();
        getAlertCore().postAlert(alert);
        return;
      }

      if (controller.getStatus().equals(Controller.STATUS_NEED_UPDATE)) {
        Log.d(TAG, "Ignoring controller: needs update.");
        final AlertCore.Alert alert = AlertCore.newBuilder("Controller disabled")
            .setDescription("Controller firmware too old; controller is disabled.")
            .severityError()
            .setId(alertId)
            .build();
        getAlertCore().postAlert(alert);
        return;
      }

      final AlertCore.Alert alert = AlertCore.newBuilder("Controller attached")
          .setDescription(String.format("Controller \"%s\" attached.", controller.getName()))
          .severityInfo()
          .setId(alertId)
          .build();
      getAlertCore().postAlert(alert);

      mExecutorService.submit(new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "Searching known controllers ...");
          try {
            for (final Models.Controller existingController : mBackend.getControllers()) {
              if (controller.getName().equals(existingController.getName())) {
                Log.d(TAG, "Success! Known controller.");
                return;
              }
            }
          } catch (BackendException e) {
            Log.w(TAG, "Error finding controller: " + e);
          }

          // Start activity, unknown controller.
          NewControllerActivity.startForNewController(mContext, controller.getName(),
              controller.getSerialNumber(), controller.getDeviceType());
        }
      });
    }

  }

}
