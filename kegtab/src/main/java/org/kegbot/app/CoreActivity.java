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

import android.app.ActionBar;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.common.collect.Lists;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.kegbot.app.alert.AlertActivity;
import org.kegbot.app.alert.AlertCancelledEvent;
import org.kegbot.app.alert.AlertCore;
import org.kegbot.app.alert.AlertCore.Alert;
import org.kegbot.app.alert.AlertPostedEvent;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.service.KegbotCoreService;
import org.kegbot.core.KegbotCore;

import java.util.List;

/**
 * An activity which starts the core service on create and resume.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class CoreActivity extends Activity {

  private static final String TAG = CoreActivity.class.getSimpleName();

  private Bus mBus;
  private AppConfiguration mConfig; // TODO(mikey):remove me after moving checkin info elsewhere
  private AlertCore mAlertCore;
  private final List<Alert> mCachedAlerts = Lists.newArrayList();

  private Menu mMenu;
  private NfcAdapter mNfcAdapter;

  private final Object mCoreListener = new Object() {
    @Subscribe
    public void onAlertPosted(AlertPostedEvent event) {
      Log.d(TAG, "Got alert posted");
      rebuildAlerts();
    }

    @Subscribe
    public void onAlertCancelledEvent(AlertCancelledEvent event) {
      rebuildAlerts();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EasyTracker.getInstance().setContext(this);
    KegbotCoreService.startService(this);

    mConfig = KegbotCore.getInstance(this).getConfiguration();
    mBus = KegbotCore.getInstance(this).getBus();
    mAlertCore = KegbotCore.getInstance(this).getAlertCore();
  }

  @Override
  protected void onStart() {
    super.onStart();
    EasyTracker.getInstance().activityStart(this);

    mConfig = KegbotCore.getInstance(this).getConfiguration();
    mBus = KegbotCore.getInstance(this).getBus();
    mAlertCore = KegbotCore.getInstance(this).getAlertCore();
    mBus.register(mCoreListener);
  }

  @Override
  protected void onStop() {
    mBus.unregister(mCoreListener);
    EasyTracker.getInstance().activityStop(this);
    mMenu = null;
    super.onStop();
  }

  @Override
  protected void onResume() {
    setupActionBar();
    if (mConfig.keepScreenOn()) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    registerNfcDispatch();
    KegbotCoreService.startService(this);
    rebuildAlerts();
    super.onResume();
  }

  @Override
  protected void onPause() {
    unregisterNfcDispatch();
    super.onPause();
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
    rebuildAlerts();
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
      case R.id.alertGeneral:
        AlertActivity.showDialogs(this);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void rebuildAlerts() {
    if (mMenu == null) {
      Log.w(TAG, "No menu, can't rebuild.");
      return;
    }

    final List<Alert> alerts = mAlertCore.getAlerts();
    if (mCachedAlerts.equals(alerts)) {
      Log.d(TAG, "No change to alerts.");
      return;
    }

    mCachedAlerts.clear();
    mCachedAlerts.addAll(alerts);

    final MenuItem item = mMenu.findItem(R.id.alertGeneral);
    if (mCachedAlerts.isEmpty()) {
      Log.d(TAG, "No alerts, hiding menu.");
      item.setVisible(false);
      return;
    }

    item.setVisible(true);
    if (mCachedAlerts.size() == 1) {
      item.setTitle(mCachedAlerts.get(0).getTitle());
    } else {
      item.setTitle(String.format("%s Alerts", String.valueOf(mCachedAlerts.size())));
    }

    mMenu.findItem(R.id.alertUpdate).setVisible(mConfig.getUpdateAvailable());
  }

  protected void updateConnectivityAlert(boolean isConnected) {
    if (mMenu != null) {
      mMenu.findItem(R.id.alertNetwork).setVisible(!isConnected);
    }
  }

  private void registerNfcDispatch() {
    mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    if (mNfcAdapter == null) {
      Log.d(TAG, "NFC is not available.");
      return;
    }

    final IntentFilter intentFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
    try {
      intentFilter.addDataType("*/*");
    } catch (MalformedMimeTypeException e) {
        throw new RuntimeException("Error creating NFC filter", e);
    }

    final String[][] techLists = new String[][] {
        new String[] { IsoDep.class.getName() },
        new String[] { MifareClassic.class.getName() },
        new String[] { MifareUltralight.class.getName() },
        new String[] { NfcA.class.getName() },
        new String[] { NfcB.class.getName() },
        new String[] { NfcF.class.getName() }
    };

    final Intent intent = AuthenticatingActivity.getStartForNfcIntent(getApplicationContext());
    final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    mNfcAdapter.enableForegroundDispatch(this, pendingIntent, null, techLists);
    Log.d(TAG, "NFC dispatch registered.");
  }

  private void unregisterNfcDispatch() {
    if (mNfcAdapter != null) {
      mNfcAdapter.disableForegroundDispatch(this);
      mNfcAdapter = null;
    }
  }

}
