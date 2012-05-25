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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.kegbot.api.KegbotApiException;
import org.kegbot.app.KegtapActivity;
import org.kegbot.app.KegtapBroadcast;
import org.kegbot.app.R;
import org.kegbot.app.setup.CheckinClient;
import org.kegbot.app.util.PreferenceHelper;
import org.kegbot.app.util.Utils;
import org.kegbot.core.AuthenticationManager;
import org.kegbot.core.AuthenticationToken;
import org.kegbot.core.ConfigurationManager;
import org.kegbot.core.Flow;
import org.kegbot.core.FlowManager;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.Tap;
import org.kegbot.core.TapManager;
import org.kegbot.core.ThermoSensor;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Models.KegTap;
import org.kegbot.proto.Models.User;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Primary service for running this kegbot.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegbotCoreService extends Service {

  private static String TAG = KegbotCoreService.class.getSimpleName();

  private static final int NOTIFICATION_FOREGROUND = 1;

  /**
   * The flow manager for the core.
   */
  private final FlowManager mFlowManager = FlowManager.getSingletonInstance();

  private final TapManager mTapManager = TapManager.getSingletonInstance();

  private final ConfigurationManager mConfigManager = ConfigurationManager.getSingletonInstance();

  private ExecutorService mFlowExecutorService;
  private PreferenceHelper mPreferences;

  private KegbotApiService mApiService;
  private boolean mApiServiceBound;

  private KegbotHardwareService mHardwareService;
  private boolean mHardwareServiceBound;

  private KegbotSoundService mSoundService;
  private boolean mSoundServiceBound;

  private final CheckinClient mCheckinClient = new CheckinClient(this);

  private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

  private final ScheduledExecutorService mScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

  private static final long CHECKIN_RETRY_DELAY_MINUTES = TimeUnit.HOURS.toMinutes(2);

  private static final long CHECKIN_INTERVAL_MINUTES = TimeUnit.HOURS.toMinutes(12);

  private long mLastPourStartUptimeMillis = 0;

  private static final long SUPPRESS_POUR_START_MILLIS = 2000;

  private final Runnable mCheckinRunnable = new Runnable() {
    @Override
    public void run() {
      long nextDelay = CHECKIN_INTERVAL_MINUTES;
      try {
        // TODO(mikey): process messages/errors/updates
        mCheckinClient.checkin();
        Log.w(TAG, "Checkin succeeded.");
      } catch (Exception e) {
        // Ignore
        Log.w(TAG, "Checkin failed: " + e);
        nextDelay = CHECKIN_RETRY_DELAY_MINUTES;
      }
      Log.d(TAG, "Next checkin attempt: " + nextDelay + " minutes");
      mScheduledExecutorService.schedule(this, nextDelay, TimeUnit.MINUTES);
    }
  };

  /**
   * Connection to the API service.
   */
  private final ServiceConnection mApiServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      mApiService = ((KegbotApiService.LocalBinder) service).getService();
      debugNotice("Core->APIService connection established.");

      mFlowExecutorService = Executors.newSingleThreadExecutor();
      mFlowExecutorService.submit(mFlowManagerWorker);
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      mApiService = null;
      debugNotice("Core->APIService connection lost.");
    }
  };

  /**
   * Connection to the hardware service.
   */
  private final ServiceConnection mHardwareServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      mHardwareService = ((KegbotHardwareService.LocalBinder) service).getService();
      debugNotice("Core->HardwareService connection established.");
      mHardwareService.attachListener(mHardwareListener);
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      mHardwareService = null;
      debugNotice("Core->HardwareService connection lost.");
    }
  };

  /**
   * Connection to the hardware service.
   */
  private final ServiceConnection mSoundServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      mSoundService = ((KegbotSoundService.LocalBinder) service).getService();
      debugNotice("Core->SoundService connection established.");
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      mSoundService = null;
      debugNotice("Core->SoundService connection lost.");
    }
  };

  /**
   * Binder interface to this service. Local binds only.
   */
  public class LocalBinder extends Binder {
    public KegbotCoreService getService() {
      return KegbotCoreService.this;
    }
  }

  private final IBinder mBinder = new LocalBinder();

  private final OnSharedPreferenceChangeListener mPreferenceListener = new OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
      if (PreferenceHelper.KEY_API_KEY.equals(key) || PreferenceHelper.KEY_RUN_CORE.equals(key)) {
        Log.d(TAG, "Shared prefs changed, relaunching core; key=" + key);
        updateFromPreferences();
      }
    }
  };

  private final Runnable mFlowManagerWorker = new Runnable() {
    @Override
    public void run() {
      Log.i(TAG, "Kegbot core starting up!");

      try {
        configure();
      } catch (KegbotApiException e1) {
        Log.e(TAG, "Api failed.", e1);
      }
    }
  };

  private final KegbotHardwareService.Listener mHardwareListener = new KegbotHardwareService.Listener() {
    @Override
    public void onTokenSwiped(AuthenticationToken token, String tapName) {
      Log.d(TAG, "Auth token swiped: " + token);
    }

    @Override
    public void onTokenRemoved(AuthenticationToken token, String tapName) {
      Log.d(TAG, "Auth token removed: " + token);
    }

    @Override
    public void onTokenAttached(final AuthenticationToken token, final String tapName) {
      Log.d(TAG, "Auth token added: " + token);
      final Intent intent = KegtapBroadcast.getAuthBeginIntent(token);
      sendBroadcast(intent);

      final Runnable r = new Runnable() {
        @Override
        public void run() {
          boolean success = false;
          String message = "";
          try {
            Log.d(TAG, "onTokenAttached: running");
            final AuthenticationManager am =
                AuthenticationManager.getSingletonInstance(KegbotCoreService.this);
            User user = am.authenticateToken(token);
            Log.d(TAG, "Authenticated user: " + user);
            if (user != null) {
              success = true;
              am.noteUserAuthenticated(user);
              for (final Tap tap : mTapManager.getTaps()) {
                mFlowManager.activateUserAtTap(tap, user.getUsername());
              }
            } else {
              message = getString(R.string.authenticating_no_access_token);
            }
          } catch (Exception e) {
            Log.e(TAG, "Exception: " + e, e);
            message = getString(R.string.authenticating_connection_error);
          }
          if (!success) {
            sendBroadcast(KegtapBroadcast.getAuthFailIntent(message));
          }
        }
      };
      mExecutorService.submit(r);
    }

    @Override
    public void onThermoSensorUpdate(final ThermoSensor sensor) {
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "Sensor update for sensor: " + sensor);
          final RecordTemperatureRequest request = RecordTemperatureRequest.newBuilder()
              .setSensorName(sensor.getName()).setTempC((float) sensor.getTemperatureC())
              .buildPartial();
          mApiService.recordTemperatureAsync(request);
        }
      };
      mExecutorService.submit(r);
    }

    @Override
    public void onMeterUpdate(final FlowMeter meter) {
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "Meter update for meter: " + meter);
          mFlowManager.handleMeterActivity(meter.getName(), (int) meter.getTicks());
        }
      };
      mExecutorService.submit(r);
    }
  };

  private final FlowManager.Listener mFlowListener = new FlowManager.Listener() {
    @Override
    public void onFlowUpdate(final Flow flow) {
      if (flow.isAuthenticated()) {
        mHardwareService.setTapRelayEnabled(flow.getTap(), true);
      }
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "Flow updated: " + flow);
          final Intent intent = KegtapBroadcast.getPourUpdateBroadcastIntent(flow);
          sendOrderedBroadcast(intent, null);
        }
      };
      mExecutorService.submit(r);
    }

    @Override
    public void onFlowStart(final Flow flow) {
      if (flow.isAuthenticated()) {
        mHardwareService.setTapRelayEnabled(flow.getTap(), true);
      }

      final long now = SystemClock.uptimeMillis();
      final long delta = now - mLastPourStartUptimeMillis;

      if (delta > SUPPRESS_POUR_START_MILLIS) {
        mLastPourStartUptimeMillis = now;
        final Runnable r = new Runnable() {
          @Override
          public void run() {
            Log.d(TAG, "Flow started: " + flow);
            final Intent intent = KegtapBroadcast.getPourStartBroadcastIntent(flow);
            sendOrderedBroadcast(intent, null);
          }
        };
        mExecutorService.submit(r);
      }
    }

    @Override
    public void onFlowEnd(final Flow flow) {
      mHardwareService.setTapRelayEnabled(flow.getTap(), false);
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "Flow ended: " + flow);
          recordDrinkForFlow(flow);
        }
      };
      mExecutorService.submit(r);
    }
  };

  @Override
  public void onCreate() {
    super.onCreate();
    mPreferences = new PreferenceHelper(getApplicationContext());
    mFlowManager.setDefaultIdleTimeMillis(mPreferences.getIdleTimeoutMs());
    Log.d(TAG, "onCreate()");
    Log.d(TAG, "Kegtap User-Agent: " + Utils.getUserAgent());
    updateFromPreferences();
    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        .registerOnSharedPreferenceChangeListener(mPreferenceListener);

    mScheduledExecutorService.submit(mCheckinRunnable);
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy()");
    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        .unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
    stop();
    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    debugNotice("Core service started.");
    return START_STICKY;
  }

  /**
   * Attaches to the running {@link KegbotApiService}.
   */
  private void bindToApiService() {
    final Intent serviceIntent = new Intent(this, KegbotApiService.class);
    bindService(serviceIntent, mApiServiceConnection, Context.BIND_AUTO_CREATE);
    mApiServiceBound = true;
  }

  /**
   * Attaches to the running {@link KegbotHardwareService}.
   */
  private synchronized void bindToHardwareService() {
    final Intent intent = new Intent(this, KegbotHardwareService.class);
    bindService(intent, mHardwareServiceConnection, Context.BIND_AUTO_CREATE);
    mHardwareServiceBound = true;
  }

  private synchronized void bindToSoundService() {
    final Intent intent = new Intent(this, KegbotSoundService.class);
    bindService(intent, mSoundServiceConnection, Context.BIND_AUTO_CREATE);
    mSoundServiceBound = true;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  public void stop() {
    mFlowManager.stop();
    mFlowManager.removeFlowListener(mFlowListener);
    if (mApiServiceBound) {
      unbindService(mApiServiceConnection);
      mApiServiceBound = false;
    }
    if (mHardwareServiceBound) {
      unbindService(mHardwareServiceConnection);
      mHardwareServiceBound = false;
    }
    if (mSoundServiceBound) {
      unbindService(mSoundServiceConnection);
      mSoundServiceBound = false;
    }
    if (mFlowExecutorService != null) {
      mFlowExecutorService.shutdown();
      mFlowExecutorService = null;
    }
  }

  private void updateFromPreferences() {
    final boolean runCore = mPreferences.getRunCore();
    if (runCore) {
      Log.d(TAG, "Running core!");
      bindToApiService();
      bindToHardwareService();
      bindToSoundService();
      mFlowManager.addFlowListener(mFlowListener);
      startForeground(NOTIFICATION_FOREGROUND, buildForegroundNotification());
    } else {
      Log.d(TAG, "No core.");
      stop();
      stopForeground(true);
    }
  }

  private Notification buildForegroundNotification() {
    final Intent intent = new Intent(this, KegtapActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
        PendingIntent.FLAG_CANCEL_CURRENT);
    final Notification notification = new Notification.Builder(this)
        .setOngoing(true)
        .setSmallIcon(R.drawable.icon)
        .setWhen(System.currentTimeMillis())
        .setContentTitle(getString(R.string.kegbot_core_running))
        .setContentIntent(pendingIntent)
        .getNotification();
    return notification;
  }

  /**
   * @param ended
   */
  private void recordDrinkForFlow(final Flow ended) {
    Log.d(TAG, "Recording drink for flow: " + ended);
    Log.d(TAG, "Tap: "  + ended.getTap());
    mApiService.recordDrinkAsync(ended);
  }

  /**
   * @throws KegbotApiException
   *
   */
  private void configure() throws KegbotApiException {
    Log.d(TAG, "Configuring!");
    final Uri apiUrl = mPreferences.getKegbotUrl();
    mApiService.setApiUrl(apiUrl.toString());
    mApiService.setApiKey(mPreferences.getApiKey());

    final List<KegTap> taps = mApiService.getKegbotApi().getAllTaps();

    Log.d(TAG, "Found " + taps.size() + " tap(s).");
    for (final KegTap tapInfo : taps) {
      Log.d(TAG, "Adding tap: " + tapInfo.getMeterName());
      final Tap tap = new Tap(tapInfo.getDescription(), tapInfo.getMlPerTick(), tapInfo
          .getMeterName(), tapInfo.getRelayName());
      mTapManager.addTap(tap);
      mConfigManager.setTapDetail(tap.getMeterName(), tapInfo);
    }
  }

  private void debugNotice(String message) {
    Log.d(TAG, message);
    Toast.makeText(KegbotCoreService.this, message, Toast.LENGTH_SHORT).show();
  }

}
