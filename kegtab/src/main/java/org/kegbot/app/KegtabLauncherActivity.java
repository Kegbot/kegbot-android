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

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.analytics.tracking.android.EasyTracker;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.setup.SetupActivity;

/**
 * Main launcher activity; simply redirects to {@link SetupActivity} or
 * {@link HomeActivity} depending on state.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegtabLauncherActivity extends Activity {

  private static final String TAG = KegtabLauncherActivity.class.getSimpleName();

  private static final int REQUEST_START_SETUP = 100;

  AppConfiguration mConfig;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mConfig = ((KegbotApplication) getApplication()).getConfig();
  }

  @Override
  protected void onStart() {
    super.onStart();
    EasyTracker.getInstance().setContext(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (isFinishing()) {
      return;
    }

    final int setupVersion = mConfig.getSetupVersion();
    if (setupVersion < SetupActivity.SETUP_VERSION) {
      Log.d(TAG, "Setup is not complete, version=" + setupVersion + "current="
          + SetupActivity.SETUP_VERSION);

      try {
        if (setupVersion == 0) {
          EasyTracker.getTracker().sendEvent("FirstInstall", "true", "", Long.valueOf(0));
          EasyTracker.getTracker().sendEvent("FirstInstall", "product", Build.PRODUCT,
              Long.valueOf(0));
          EasyTracker.getTracker().sendEvent("FirstInstall", "device", Build.DEVICE,
              Long.valueOf(0));
        } else {
          EasyTracker.getTracker().sendEvent("Upgrade",
              String.format("ToVersion %s", Integer.valueOf(SetupActivity.SETUP_VERSION)),
              "", Long.valueOf(0));
        }
      } catch (Exception e) {
        // Ignore any stupid analytics crashes.
      }

      final Intent setupIntent = new Intent(this, SetupActivity.class);
      if (setupVersion > 0) {
        setupIntent.putExtra(SetupActivity.EXTRA_REASON, SetupActivity.EXTRA_REASON_UPGRADE);
      }
      startActivityForResult(setupIntent, REQUEST_START_SETUP);
    } else {
      Log.d(TAG, "Starting main activity");
      final Intent mainIntent = new Intent(this, HomeActivity.class);
      mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(mainIntent);
      finish();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_START_SETUP:
        if (resultCode == RESULT_OK) {
          Log.i(TAG, "Setup completed successfully, returning to main.");
          return;
        } else {
          Log.i(TAG, "Setup aborted, finishing.");
          finish();
          return;
        }
      default:
        Log.w(TAG, "Unknown requestCode: " + requestCode);
        finish();
        return;
    }
  }

}
