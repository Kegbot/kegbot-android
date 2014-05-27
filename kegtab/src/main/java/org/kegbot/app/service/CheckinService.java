/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;

import org.kegbot.app.KegbotApplication;
import org.kegbot.app.R;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.util.CheckinClient;
import org.kegbot.app.util.Utils;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Checkin service: pings kegbot servers for version/support information.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class CheckinService extends IntentService {

  private static final String TAG = CheckinService.class.getSimpleName();
  static final String CHECKIN_NOW_ACTION = "org.kegbot.app.CHECKIN";

  private static final long CHECKIN_INTERVAL_MILLIS = AlarmManager.INTERVAL_HALF_DAY;

  private static final int CHECKIN_NOTIFICATION_ID = 100;

  private AppConfiguration mConfig;
  private PendingIntent mPendingIntent;
  private WakeLock mWakeLock;

  public CheckinService() {
    super("CheckinService");
  }

  @Override
  public void onCreate() {
    super.onCreate();

    mConfig = ((KegbotApplication) getApplication()).getConfig();
    final PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kbcheckin");

    registerAlarm();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    acquireWakeLock();
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    // If action was unset, it means the service was started
    // only to schedule the next checkin.
    try {
      if (CHECKIN_NOW_ACTION.equals(intent.getAction())) {
        doCheckin();
      }
    } finally {
      releaseWakeLock();
    }
  }

  @Override
  protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    super.dump(fd, writer, args);
    writer.println(
        String.format("Last checkin attempt: %s", new Date(mConfig.getLastCheckinAttempt())));
    writer.println(
        String.format("Last checkin success: %s", new Date(mConfig.getLastCheckinSuccess())));
  }

  private void registerAlarm() {
    unregisterAlarm();
    Log.d(TAG, "Registering alarm.");
    final Intent intent = getCheckinNowIntent(this);
    mPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
    final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
    final long nextCheckin = SystemClock.elapsedRealtime() + CHECKIN_INTERVAL_MILLIS;
    alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextCheckin,
        CHECKIN_INTERVAL_MILLIS, mPendingIntent);
  }

  private void unregisterAlarm() {
    if (mPendingIntent != null) {
      Log.d(TAG, "Unregistering alarm.");
      final AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
      alarmManager.cancel(mPendingIntent);
      mPendingIntent = null;
    }
  }

  private void acquireWakeLock() {
    synchronized (mWakeLock) {
      if (!mWakeLock.isHeld()) {
        Log.d(TAG, "Acquiring wake lock.");
        mWakeLock.acquire();
      }
    }
  }

  private void releaseWakeLock() {
    synchronized (mWakeLock) {
      if (mWakeLock.isHeld()) {
        Log.d(TAG, "Releasing wake lock.");
        mWakeLock.release();
      }
    }
  }

  private void doCheckin() {
    final CheckinClient client = CheckinClient.fromContext(getApplicationContext());
    try {
      client.checkin();
    } catch (IOException e) {
      Log.w(TAG, "Checkin failed.", e);
      return;
    }

    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    if (mConfig.getUpdateAvailable()) {
      Log.d(TAG, "Update is available, notifying..");
      final boolean updateRequired = mConfig.getUpdateRequired();

      Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
      notificationIntent.setData(Uri.parse("market://details?id=org.kegbot.app"));
      PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

      int titleRes = updateRequired ? R.string.checkin_update_required_title
          : R.string.checkin_update_available_title;

      final Notification.Builder builder = new Notification.Builder(this)
          .setSmallIcon(updateRequired ? R.drawable.icon_warning : R.drawable.icon_download)
          .setContentTitle(getString(titleRes))
          .setContentText(getString(R.string.checkin_update_description))
          .setContentIntent(contentIntent)
          .setOnlyAlertOnce(true)
          .setAutoCancel(true);

      final Notification notification = Utils.buildNotification(builder);

      Log.d(TAG, "Posting notification.");
      nm.notify(CHECKIN_NOTIFICATION_ID, notification);
    } else {
      nm.cancel(CHECKIN_NOTIFICATION_ID);
    }
  }

  private static Intent getCheckinNowIntent(Context context) {
    final Intent intent = new Intent(CHECKIN_NOW_ACTION);
    return intent;
  }

  public static void requestImmediateCheckin(Context context) {
    Log.d(TAG, "Requesting immediate checkin.");
    final Intent intent = getCheckinNowIntent(context);
    context.sendBroadcast(intent);
  }

  public static void startCheckinService(Context context, boolean checkinNow) {
    final Intent intent = new Intent(context, CheckinService.class);
    if (checkinNow) {
      intent.setAction(CHECKIN_NOW_ACTION);
    }
    context.startService(intent);
  }

}
