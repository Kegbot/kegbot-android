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
package org.kegbot.app.service;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.kegbot.app.R;
import org.kegbot.app.build.BuildInfo;
import org.kegbot.app.util.DeviceId;
import org.kegbot.app.util.PreferenceHelper;
import org.kegbot.app.util.Utils;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.collect.Lists;

/**
 * Checkin service: pings kegbot servers for version/support information.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class CheckinService extends IntentService {

  private static final String TAG = CheckinService.class.getSimpleName();
  private static final String CHECKIN_URL = "https://kegbot.org/checkin/";
  static final String CHECKIN_ACTION = "org.kegbot.app.CHECKIN";
  static final String CHECKIN_COMPLETE_ACTION = "org.kegbot.app.CHECKIN_COMPLETE";
  private static final long CHECKIN_INTERVAL_MILLIS = AlarmManager.INTERVAL_HALF_DAY;

  private static final int CHECKIN_NOTIFICATION_ID = 100;

  /**
   * Normal checkin response status; nothing to do.
   */
  static final String STATUS_OK = "ok";

  /**
   * Checkin response status indicating that an upgrade is available.
   */
  static final String STATUS_UPGRADE_AVAILABLE = "upgrade-available";

  /**
   * Checkin response status indicating that an upgrade is required.
   */
  static final String STATUS_UPGRADE_REQUIRED = "upgrade-required";

  /**
   * Checkin response status indicating that this device is not supported.
   */
  static final String STATUS_NOT_SUPPORTED = "not-supported";
  private PreferenceHelper mPrefsHelper;

  private int mKegbotVersion = -1;
  private String mDeviceId;
  private PendingIntent mPendingIntent;
  private WakeLock mWakeLock;

  public CheckinService() {
    super("CheckinService");
  }

  @Override
  public void onCreate() {
    super.onCreate();

    mPrefsHelper = new PreferenceHelper(this);

    final PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kbcheckin");

    mDeviceId = DeviceId.getDeviceId(this);
    try {
      final PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      mKegbotVersion = pinfo.versionCode;
    } catch (NameNotFoundException e) {
      Log.w(TAG, "Could not look up own package info.");
    }

    registerAlarm();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    acquireWakeLock();
    return super.onStartCommand(intent, flags, startId);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    try {
      final String action = intent.getAction();
      if (!CHECKIN_ACTION.equals(action)) {
        Log.w(TAG, "Unknown intent action: " + action);
        return;
      }
      doCheckin();
    } finally {
      releaseWakeLock();
    }
  }

  @Override
  protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
    super.dump(fd, writer, args);
    writer.println(
        String.format("Last checkin attempt: %s", new Date(mPrefsHelper.getLastCheckinAttempt())));
    writer.println(
        String.format("Last checkin success: %s", new Date(mPrefsHelper.getLastCheckinSuccess())));
    writer.println(String.format("Last response:"));
    writer.println(mPrefsHelper.getLastCheckinResponse().toString());
  }

  private void registerAlarm() {
    unregisterAlarm();
    Log.d(TAG, "Registering alarm.");
    final Intent intent = getCheckinIntent(this);
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
    Log.d(TAG, "Performing checkin: " + CHECKIN_URL);
    final long now = System.currentTimeMillis();
    mPrefsHelper.setLastCheckinAttempt(now);

    final HttpClient client = new DefaultHttpClient();
    final HttpPost request = new HttpPost(CHECKIN_URL);
    final HttpParams requestParams = new BasicHttpParams();


    HttpProtocolParams.setUserAgent(requestParams, Utils.getUserAgent());
    request.setParams(requestParams);

    try {
      List<NameValuePair> params = Lists.newArrayList();
      params.add(new BasicNameValuePair("kbid", mDeviceId));
      params.add(new BasicNameValuePair("android_version", Build.VERSION.SDK));
      params.add(new BasicNameValuePair("android_device", Build.DEVICE));
      params.add(new BasicNameValuePair("kegbot_version", String.valueOf(mKegbotVersion)));
      params.add(new BasicNameValuePair("kegbot_date", BuildInfo.BUILD_DATE_HUMAN));
      params.add(new BasicNameValuePair("gcm_reg_id", mPrefsHelper.getGcmRegistrationId()));
      request.setEntity(new UrlEncodedFormEntity(params));

      final HttpResponse response = client.execute(request);
      Log.d(TAG, "Checkin complete");
      final String responseBody = EntityUtils.toString(response.getEntity());
      final ObjectMapper mapper = new ObjectMapper();
      final JsonNode rootNode = mapper.readValue(responseBody, JsonNode.class);
      if (response.getStatusLine().getStatusCode() == 200) {
        mPrefsHelper.setLastCheckinSuccess(now);
        mPrefsHelper.setIsRegistered(true);
        mPrefsHelper.setLastCheckinResponse(rootNode);
      }
    } catch (IOException e) {
      Log.d(TAG, "Checkin failed: " + e);
    }
    processLastCheckinResponse();
  }

  /**
   * Processes the checkin response message.
   *
   * A well-formed response will have the following attributes:
   * <ul>
   * <li><code>status</code>: a string status code.
   * <li><code>title</code>: title of the message shown (optional)
   * <li><code>description</code>: body of the message shown (optional)
   * </ul>
   *
   * <code>title</code> and <code>description</code> are meaningful only if
   * <code>status</code> is not {@value #STATUS_OK}.
   *
   * @see #STATUS_OK
   * @see #STATUS_UPGRADE_AVAILABLE
   * @see #STATUS_UPGRADE_REQUIRED
   * @see #STATUS_NOT_SUPPORTED
   */
  private void processLastCheckinResponse() {
    final JsonNode response = mPrefsHelper.getLastCheckinResponse();
    Log.d(TAG, "Checkin response: " + response);
    NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

    final JsonNode statusNode = response.get("status");
    if (statusNode == null || !statusNode.isTextual()) {
      Log.d(TAG, "Invalid checkin response: no status.");
      return;
    }

    final String status = statusNode.getTextValue();
    if (STATUS_OK.equals(status) || STATUS_UPGRADE_AVAILABLE.equals(status)
        || STATUS_UPGRADE_REQUIRED.equals(status) || STATUS_NOT_SUPPORTED.equals(status)) {
      Log.d(TAG, "Checkin status: " + status);
    } else {
      Log.d(TAG, "Invalid checkin response: unknown status: " + status);
      return;
    }

    final String lastStatus = mPrefsHelper.getLastCheckinStatus();
    mPrefsHelper.setLastCheckinStatus(status);

    if (!STATUS_OK.equals(status)) {
      if (STATUS_UPGRADE_AVAILABLE.equals(status) || STATUS_UPGRADE_REQUIRED.equals(status)) {
        Intent notificationIntent = new Intent(Intent.ACTION_VIEW);
        notificationIntent.setData(Uri.parse("market://details?id=org.kegbot.app"));
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        int titleRes = STATUS_UPGRADE_AVAILABLE.equals(status) ? R.string.checkin_update_available_title : R.string.checkin_update_required_title;

        Notification noti = new Notification.Builder(this)
          .setSmallIcon(R.drawable.warning_icon)
          .setContentTitle(getString(titleRes))
          .setContentText(getString(R.string.checkin_update_description))
          .setContentIntent(contentIntent)
          .setOngoing(true)
          .setOnlyAlertOnce(true)
          .getNotification();

        Log.d(TAG, "Posting notification.");
        nm.notify(CHECKIN_NOTIFICATION_ID, noti);

      } else {
        nm.cancel(CHECKIN_NOTIFICATION_ID);
      }
    }

    final Intent intent = new Intent(CHECKIN_COMPLETE_ACTION);
    intent.putExtra("status", status);
    intent.putExtra("last-status", lastStatus);
    sendBroadcast(intent);
  }

  private static Intent getCheckinIntent(Context context) {
    final Intent intent = new Intent(CHECKIN_ACTION);
    return intent;
  }

  public static void requestImmediateCheckin(Context context) {
    final Intent intent = getCheckinIntent(context);
    context.sendBroadcast(intent);
  }

  public static void startCheckinService(Context context) {
    final Intent intent = new Intent(context, CheckinService.class);
    intent.setAction(CHECKIN_ACTION);
    context.startService(intent);
  }

}
