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

/**
 *
 */
package org.kegbot.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.config.SharedPreferencesConfigurationStore;
import org.kegbot.app.service.CheckinService;
import org.kegbot.app.util.Utils;

/**
 * Kegbot customized application.
 */
public class KegbotApplication extends MultiDexApplication {

  private static final String TAG = KegbotApplication.class.getSimpleName();

  private static final String RELEASE_SIGNATURE = "06D936CB1BB9FB1A6BD4FC80105BDD79A5AF137F";

  private boolean mReleaseBuild = !BuildConfig.DEBUG;

  private SharedPreferences mSharedPreferences;
  private AppConfiguration mConfig;

  @Override
  public void onCreate() {
    super.onCreate();
    Log.i(TAG, "Kegbot starting.");

    if (!BuildConfig.DEBUG) {
      final PackageInfo packageInfo = Utils.getOwnPackageInfo(this);
      mReleaseBuild &= Utils.packageMatchesFingerprint(packageInfo, RELEASE_SIGNATURE);
    }
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    mConfig = new AppConfiguration(new SharedPreferencesConfigurationStore(mSharedPreferences));

    if (mReleaseBuild) {
      Log.d(TAG, "Activating crashlytics ...");
      try {
        Crashlytics.setUserIdentifier(mConfig.getRegistrationId());
        Log.d(TAG, "Crashlytics activated.");
      } catch (Exception e) {
        Log.w(TAG, "Crashlytics not activated: " + e, e);
      } catch (NoClassDefFoundError e) {
        Log.w(TAG, "Crashlytics not activated: " + e, e);
      }
    }

    final String userAgent = Utils.getUserAgent(getApplicationContext());
    Log.d(TAG, "Kegtab User-agent: " + userAgent);
    System.setProperty("http.agent", userAgent);

    CheckinService.startCheckinService(this, false);
  }

  public AppConfiguration getConfig() {
    return mConfig;
  }

  public SharedPreferences getSharedPreferences() {
    return mSharedPreferences;
  }

  public boolean isReleaseBuild() {
    return mReleaseBuild;
  }

  public static KegbotApplication get(final Context context) {
    return (KegbotApplication) context.getApplicationContext();
  }

}
