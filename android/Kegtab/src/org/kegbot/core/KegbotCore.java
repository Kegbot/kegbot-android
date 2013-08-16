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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Set;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.config.SharedPreferencesConfigurationStore;
import org.kegbot.app.util.DeviceId;
import org.kegbot.app.util.ImageDownloader;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.app.util.Utils;
import org.kegbot.core.FlowManager.Clock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * Top-level class implementing the Kegbot core.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class KegbotCore {

  private static final String TAG = KegbotCore.class.getSimpleName();

  private static KegbotCore sInstance;

  private final Bus mBus;
  private final Handler mBusHandler = new Handler();

  private final SharedPreferences mSharedPreferences;
  private final AppConfiguration mConfig;

  private final Set<Manager> mManagers = Sets.newLinkedHashSet();
  private final TapManager mTapManager;
  private final FlowManager mFlowManager;
  private final AuthenticationManager mAuthenticationManager;
  private final SoundManager mSoundManager;
  private final ImageDownloader mImageDownloader;

  private final KegbotApi mApi;
  private final SyncManager mSyncManager;

  private final KegboardManager mKegboardManager;
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

    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    mConfig = new AppConfiguration(new SharedPreferencesConfigurationStore(mSharedPreferences));

    mApi = new KegbotApiImpl();
    mApi.setApiUrl(mConfig.getApiUrl());
    mApi.setApiKey(mConfig.getApiKey());

    mImageDownloader = new ImageDownloader(context, mConfig.getKegbotUrl());

    mTapManager = new TapManager(mBus);
    mManagers.add(mTapManager);

    mFlowManager = new FlowManager(mBus, mTapManager, mConfig, mClock);
    mManagers.add(mFlowManager);

    mSyncManager = new SyncManager(mBus, context, mApi);
    mManagers.add(mSyncManager);

    mKegboardManager = new KegboardManager(mBus, context);
    mManagers.add(mKegboardManager);

    mHardwareManager = new HardwareManager(mBus, context, mConfig, mKegboardManager);
    mManagers.add(mHardwareManager);

    mAuthenticationManager = new AuthenticationManager(mBus, context, mApi, mConfig);
    mManagers.add(mAuthenticationManager);

    mSoundManager = new SoundManager(mBus, context);
    mManagers.add(mSoundManager);

    mBluetoothManager = new BluetoothManager(mBus, context);
    mManagers.add(mBluetoothManager);
  }

  public synchronized void start() {
    if (!mStarted) {
      for (final Manager manager : mManagers) {
        Log.d(TAG, "Starting " + manager.getName());
        manager.start();
      }
      mStarted = true;
    }
  }

  public synchronized void stop() {
    if (mStarted) {
      for (final Manager manager : mManagers) {
        Log.d(TAG, "Stopping " + manager.getName());
        manager.stop();
      }
      mStarted = false;
    }
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
   * @return the api
   */
  public KegbotApi getApi() {
    return mApi;
  }

  /**
   * @return the api manager
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
   * @return the kegboardManager
   */
  public KegboardManager getKegboardManager() {
    return mKegboardManager;
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
    writer.printPair("fingerprint", Build.FINGERPRINT).println();
    writer.printPair("board", Build.BOARD).println();
    writer.printPair("device", Build.DEVICE).println();
    writer.printPair("model", Build.MODEL).println();
    writer.printPair("manufacturer", Build.MANUFACTURER).println();
    writer.printPair("sdk", Build.VERSION.SDK).println();
    writer.decreaseIndent();
    writer.println();

    try {
      PackageManager pm = mContext.getPackageManager();
      PackageInfo packageInfo;
      try {
        packageInfo = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_SIGNATURES);
      } catch (NameNotFoundException e) {
        throw new RuntimeException("Cannot get own package info.", e);
      }

      writer.println("## Package info");
      writer.println();
      writer.increaseIndent();
      writer.printPair("versionName", packageInfo.versionName).println();
      writer.printPair("versionCode", String.valueOf(packageInfo.versionCode)).println();
      writer.printPair("packageName", packageInfo.packageName).println();
      writer.printPair("installTime", new Date(packageInfo.firstInstallTime)).println();
      writer.printPair("lastUpdateTime", new Date(packageInfo.lastUpdateTime)).println();
      writer.printPair("installerPackageName", pm.getInstallerPackageName(mContext.getPackageName()))
          .println();
      if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
        writer.printPair("signature", Utils.getFingerprintForSignature(packageInfo.signatures[0]))
            .println();
      }
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

}
