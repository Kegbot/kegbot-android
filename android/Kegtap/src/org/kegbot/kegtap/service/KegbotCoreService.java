/**
 * CONFIDENTIAL -- NOT OPEN SOURCE
 */
package org.kegbot.kegtap.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kegbot.api.KegbotApiException;
import org.kegbot.core.AuthenticationToken;
import org.kegbot.core.ConfigurationManager;
import org.kegbot.core.Flow;
import org.kegbot.core.FlowManager;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.Tap;
import org.kegbot.core.TapManager;
import org.kegbot.core.ThermoSensor;
import org.kegbot.kegtap.KegtapActivity;
import org.kegbot.kegtap.KegtapBroadcast;
import org.kegbot.kegtap.R;
import org.kegbot.kegtap.service.KegbotApiService.ConnectionState;
import org.kegbot.kegtap.util.PreferenceHelper;
import org.kegbot.proto.Api;
import org.kegbot.proto.Api.RecordTemperatureRequest;
import org.kegbot.proto.Api.TapDetailSet;
import org.kegbot.proto.Models;

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
import android.os.Handler;
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
public class KegbotCoreService extends Service implements KegbotCoreServiceInterface {

  private static String TAG = KegbotCoreService.class.getSimpleName();

  private static final int NOTIFICATION_FOREGROUND = 1;

  /**
   * The flow manager for the core.
   */
  private final FlowManager mFlowManager = FlowManager.getSingletonInstance();

  private final TapManager mTapManager = TapManager.getSingletonInstance();

  private final ConfigurationManager mConfigManager = ConfigurationManager.getSingletonInstance();

  private ExecutorService mExecutorService;
  private SharedPreferences mPreferences;

  private KegbotApiService mApiService;
  private boolean mApiServiceBound;
  private KegbotHardwareService mHardwareService;
  private boolean mHardwareServiceBound;

  private final Handler mHandler = new Handler();

  /**
   * Connection to the API service.
   */
  private ServiceConnection mApiServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      mApiService = ((KegbotApiService.LocalBinder) service).getService();
      debugNotice("Core->APIService connection established.");
      mApiService.attachListener(mApiListener);

      mExecutorService = Executors.newSingleThreadExecutor();
      mExecutorService.submit(mFlowManagerWorker);
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
  private ServiceConnection mHardwareServiceConnection = new ServiceConnection() {
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
      updateFromPreferences();
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

  private final KegbotApiService.Listener mApiListener = new KegbotApiService.Listener() {
    @Override
    public void onConnectionStateChange(ConnectionState newState) {
      // TODO
    }

    @Override
    public void onConfigurationUpdate() {
      // TODO
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
    public void onTokenAttached(AuthenticationToken token, String tapName) {
      Log.d(TAG, "Auth token added: " + token);
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
      mHandler.post(r);
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
      mHandler.post(r);
    }
  };

  private final FlowManager.Listener mFlowListener = new FlowManager.Listener() {
    @Override
    public void onFlowUpdate(final Flow flow) {
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "Flow updated: " + flow);
          final Intent intent = KegtapBroadcast.getPourUpdateBroadcastIntent(flow);
          sendOrderedBroadcast(intent, null);
        }
      };
      mHandler.post(r);
    }

    @Override
    public void onFlowStart(final Flow flow) {
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "Flow started: " + flow);
          final Intent intent = KegtapBroadcast.getPourStartBroadcastIntent(flow);
          sendOrderedBroadcast(intent, null);
        }
      };
      mHandler.post(r);
    }

    @Override
    public void onFlowEnd(final Flow flow) {
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "Flow ended: " + flow);
          recordDrinkForFlow(flow);
        }
      };
      mHandler.post(r);
    }
  };

  @Override
  public void onCreate() {
    super.onCreate();
    mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    mPreferences.registerOnSharedPreferenceChangeListener(mPreferenceListener);
    final PreferenceHelper helper = new PreferenceHelper(mPreferences);
    mFlowManager.setDefaultIdleTimeMillis(helper.getIdleTimeoutMs());

    Log.d(TAG, "onCreate()");
    updateFromPreferences();
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy()");
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
    startService(new Intent(this, KegbotHardwareService.class));
    bindService(new Intent(KegbotCoreService.this, KegbotHardwareService.class),
        mHardwareServiceConnection, Context.BIND_AUTO_CREATE);
    Log.d(TAG, "Bound to hardware service.");
    mHardwareServiceBound = true;
  }

  private synchronized void unbindFromHardwareService() {
    if (mHardwareServiceBound) {
      unbindService(mHardwareServiceConnection);
      stopService(new Intent(this, KegbotHardwareService.class));
      Log.d(TAG, "Unbound from hardware service");
      mHardwareServiceBound = false;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  private void stop() {
    mFlowManager.stop();
    mFlowManager.removeFlowListener(mFlowListener);
    if (mApiServiceBound) {
      unbindService(mApiServiceConnection);
      mApiServiceBound = false;
    }
    unbindFromHardwareService();
    if (mExecutorService != null) {
      mExecutorService.shutdown();
      mExecutorService = null;
    }
  }

  private void updateFromPreferences() {
    PreferenceHelper helper = new PreferenceHelper(mPreferences);
    final boolean runCore = helper.getRunCore();
    if (runCore) {
      Log.d(TAG, "Running core!");
      bindToApiService();
      bindToHardwareService();
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
    final Notification.Builder builder = new Notification.Builder(this);
    builder.setOngoing(true).setSmallIcon(R.drawable.icon).setWhen(SystemClock.uptimeMillis())
        .setContentTitle("Kegbot Core is running").setContentIntent(pendingIntent);
    return builder.getNotification();
  }

  /**
   * @param ended
   */
  private void recordDrinkForFlow(final Flow ended) {
    Log.d(TAG, "Recording dring for flow: " + ended);
    final PreferenceHelper helper = new PreferenceHelper(mPreferences);
    final long minVolume = helper.getMinimumVolumeMl();
    if (ended.getVolumeMl() < minVolume) {
      Log.d(TAG, "Not recording flow: "
          + "volume (" + ended.getVolumeMl() + " mL) is less than minimum "
          + "(" + minVolume + " mL)");
      return;
    }
    mApiService.recordDrinkAsync(ended);
  }

  /**
   * @throws KegbotApiException
   *
   */
  private void configure() throws KegbotApiException {
    Log.d(TAG, "Configuring!");
    final PreferenceHelper prefs = new PreferenceHelper(mPreferences);
    final Uri apiUrl = prefs.getKegbotUrl();
    mApiService.setApiUrl(apiUrl.toString());

    final String username = prefs.getUsername();
    final String password = prefs.getPassword();
    mApiService.setAccountCredentials(username, password);

    final TapDetailSet taps = mApiService.getAllTaps();
    Log.d(TAG, "Taps: " + taps);
    for (final Api.TapDetail tapDetail : taps.getTapsList()) {
      Models.KegTap tapInfo = tapDetail.getTap();
      Log.d(TAG, "Adding tap: " + tapInfo.getDescription());
      final Tap tap = new Tap(tapInfo.getDescription(), tapInfo.getMlPerTick(), tapInfo
          .getMeterName(), tapInfo.getRelayName());
      mTapManager.addTap(tap);
      mConfigManager.setTapDetail(tap.getMeterName(), tapDetail);
    }
  }

  private void debugNotice(String message) {
    Log.d(TAG, message);
    Toast.makeText(KegbotCoreService.this, message, Toast.LENGTH_SHORT).show();
  }

  @Override
  public FlowManager getFlowManager() {
    return mFlowManager;
  }

  @Override
  public TapManager getTapManager() {
    return mTapManager;
  }

}
