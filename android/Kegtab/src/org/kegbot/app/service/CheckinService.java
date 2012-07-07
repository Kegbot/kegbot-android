/**
 *
 */
package org.kegbot.app.service;

import java.io.IOException;
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
import org.kegbot.app.build.BuildInfo;
import org.kegbot.app.util.DeviceId;
import org.kegbot.app.util.PreferenceHelper;
import org.kegbot.app.util.Utils;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.collect.Lists;

/**
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class CheckinService extends IntentService {

  private static final String TAG = CheckinService.class.getSimpleName();
  private static final String CHECKIN_URL = "http://checkin.kegbot.org/checkin/";
  private static final String CHECKIN_ACTION = "org.kegbot.app.CHECKIN";
  private static final long CHECKIN_INTERVAL_MILLIS = AlarmManager.INTERVAL_HALF_DAY;

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

      final JsonNode result = doCheckin();
      if (result != null) {
        processCheckinResponse(result);
      }
    } finally {
      releaseWakeLock();
    }
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

  private JsonNode doCheckin() {
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
      }
      return rootNode;
    } catch (IOException e) {
      Log.d(TAG, "Checkin failed: " + e);
      return null;
    }
  }

  private void processCheckinResponse(JsonNode response) {
    Log.d(TAG, "Checkin response: " + response);
    // TODO(mikey): Post notification about updates.
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
