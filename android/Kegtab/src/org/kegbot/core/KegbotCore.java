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
import java.util.Set;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiImpl;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.app.util.PreferenceHelper;
import org.kegbot.core.FlowManager.Clock;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.Sets;

/**
 * Top-level class implementing the Kegbot core.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class KegbotCore {

  private static final String TAG = KegbotCore.class.getSimpleName();

  private static KegbotCore sInstance;

  private final Set<Manager> mManagers = Sets.newLinkedHashSet();
  private final PreferenceHelper mPreferences;

  private final TapManager mTapManager;
  private final FlowManager mFlowManager;
  private final AuthenticationManager mAuthenticationManager;
  private final ConfigurationManager mConfigurationManager;
  private final SoundManager mSoundManager;

  private final KegbotApi mApi;
  private final SyncManager mSyncManager;

  private final KegboardManager mKegboardManager;
  private final HardwareManager mHardwareManager;

  private final BluetoothManager mBluetoothManager;

  private boolean mStarted = false;

  private final FlowManager.Clock mClock = new Clock() {
    @Override
    public long currentTimeMillis() {
      return System.currentTimeMillis();
    }
  };

  public KegbotCore(Context context) {
    mPreferences = new PreferenceHelper(context);

    mApi = new KegbotApiImpl();
    mApi.setApiUrl(mPreferences.getApiUrl());
    mApi.setApiKey(mPreferences.getApiKey());

    mTapManager = new TapManager();
    mManagers.add(mTapManager);

    mFlowManager = new FlowManager(mTapManager, mClock);
    mManagers.add(mFlowManager);

    mSyncManager = new SyncManager(context, mApi, mPreferences);
    mManagers.add(mSyncManager);

    mKegboardManager = new KegboardManager(context);
    mManagers.add(mKegboardManager);

    mHardwareManager = new HardwareManager(context, mKegboardManager);
    mManagers.add(mHardwareManager);

    mAuthenticationManager = new AuthenticationManager(context, mApi);
    mManagers.add(mAuthenticationManager);

    mConfigurationManager = new ConfigurationManager();
    mManagers.add(mConfigurationManager);

    mSoundManager = new SoundManager(context, mApi, mFlowManager);
    mManagers.add(mSoundManager);

    mBluetoothManager = new BluetoothManager(context);
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

  /**
   * @return the preferences
   */
  public PreferenceHelper getPreferences() {
    return mPreferences;
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

  /**
   * @return the configurationManager
   */
  public ConfigurationManager getConfigurationManager() {
    return mConfigurationManager;
  }

  public void dump(PrintWriter printWriter) {
    StringWriter writer = new StringWriter();
    IndentingPrintWriter newWriter = new IndentingPrintWriter(writer, "  ");

    newWriter.printPair("mStarted: ", Boolean.valueOf(mStarted));
    newWriter.println();
    newWriter.println();

    for (final Manager manager : mManagers) {
      newWriter.printf("## %s\n", manager.getName());
      newWriter.increaseIndent();
      manager.dump(newWriter);
      newWriter.decreaseIndent();
      newWriter.println();
    }
    printWriter.write(writer.toString());
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
