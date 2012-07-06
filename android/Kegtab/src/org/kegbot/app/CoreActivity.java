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

import org.kegbot.app.service.KegbotCoreService;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.apps.analytics.easytracking.EasyTracker;

/**
 * An activity which starts the core service on create and resume.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class CoreActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EasyTracker.getTracker().setContext(this);
    KegbotCoreService.startService(this);
  }

  @Override
  protected void onStart() {
    super.onStart();
    EasyTracker.getTracker().trackActivityStart(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    EasyTracker.getTracker().trackActivityStop(this);
  }

  @Override
  protected void onResume() {
    setupActionBar();
    KegbotCoreService.startService(this);
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
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

}
