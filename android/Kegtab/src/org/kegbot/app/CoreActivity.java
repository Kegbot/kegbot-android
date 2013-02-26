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

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.service.KegbotCoreService;
import org.kegbot.core.KegbotCore;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.google.android.apps.analytics.easytracking.EasyTracker;

/**
 * An activity which starts the core service on create and resume.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class CoreActivity extends Activity {

  private static final String TAG = CoreActivity.class.getSimpleName();

  private Menu mMenu;
  private AppConfiguration mConfig; // TODO(mikey):remove me after moving checkin info elsewhere

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EasyTracker.getTracker().setContext(this);
    KegbotCoreService.startService(this);
    mConfig = KegbotCore.getInstance(this).getConfiguration();
  }

  @Override
  protected void onStart() {
    super.onStart();
    EasyTracker.getTracker().trackActivityStart(this);
    Log.d(TAG, "Registering with bus.");
    KegbotCore.getInstance(this).getBus().register(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    EasyTracker.getTracker().trackActivityStop(this);
    KegbotCore.getInstance(this).getBus().unregister(this);
    mMenu = null;
  }

  @Override
  protected void onResume() {
    setupActionBar();
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    KegbotCoreService.startService(this);
    updateAlerts();
    super.onResume();
  }

  protected void setupActionBar() {
    final ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_USE_LOGO);
      actionBar.setTitle("");
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mMenu = menu;
    getMenuInflater().inflate(R.menu.main, menu);
    updateAlerts();
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        return true;
      case R.id.alertUpdate:
        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
        marketIntent.setData(Uri.parse("market://details?id=org.kegbot.app"));
        PinActivity.startThroughPinActivity(this, marketIntent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void updateAlerts() {
    if (mMenu != null) {
      mMenu.findItem(R.id.alertUpdate).setVisible(mConfig.getUpdateNeeded());
    }
  }

}
