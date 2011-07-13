package org.kegbot.kegtap.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.kegbot.api.KegbotApiException;
import org.kegbot.core.AuthenticationToken;
import org.kegbot.core.Flow;
import org.kegbot.core.FlowManager;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.Tap;
import org.kegbot.core.TapManager;
import org.kegbot.core.ThermoSensor;
import org.kegbot.kegtap.KegtapActivity;
import org.kegbot.kegtap.R;
import org.kegbot.kegtap.service.KegbotApiService.ConnectionState;
import org.kegbot.kegtap.util.KegbotDescriptor;
import org.kegbot.kegtap.util.PreferenceUtils;
import org.kegbot.proto.Api;
import org.kegbot.proto.Api.TapDetailSet;
import org.kegbot.proto.Models;
import org.kegbot.proto.Models.Drink;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
  private FlowManager mFlowManager;

  private TapManager mTapManager;

  private ExecutorService mExecutorService;
  private SharedPreferences mPreferences;

  private KegbotApiService mApiService;
  private boolean mApiServiceBound;
  private KegbotHardwareService mHardwareService;
  private boolean mHardwareServiceBound;

  /**
   * Connection to the API service.
   */
  private ServiceConnection mApiServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      mApiService = ((KegbotApiService.LocalBinder) service).getService();
      debugNotice("Core->APIService connection established.");
      mExecutorService.submit(mFlowManagerWorker);
      mApiService.attachListener(mApiListener);
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
   * Binder interface to this service.  Local binds only.
   */
  public class LocalBinder extends Binder {
    public KegbotCoreService getService() {
      return KegbotCoreService.this;
    }
  }

  private final IBinder mBinder = new LocalBinder();

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
    }

    @Override
    public void onTokenRemoved(AuthenticationToken token, String tapName) {
    }

    @Override
    public void onTokenAttached(AuthenticationToken token, String tapName) {
    }

    @Override
    public void onThermoSensorUpdate(ThermoSensor sensor) {
      Log.d(TAG, "Sensor update for sensor: " + sensor);
      try {
        mApiService.recordTemperature(sensor.getName(), sensor.getTemperatureC());
      } catch (KegbotApiException e) {
        Log.e(TAG, "Error recording temperature", e);
      }
    }

    @Override
    public void onMeterUpdate(FlowMeter meter) {
      Log.d(TAG, "Meter update for meter: " + meter);
      mFlowManager.handleMeterActivity(meter.getName(), (int) meter.getTicks());
    }
  };

  private final FlowManager.Listener mFlowListener = new FlowManager.Listener() {
    @Override
    public void onFlowUpdate(Flow flow) {
      Log.d(TAG, "Flow updated: " + flow);
    }

    @Override
    public void onFlowStart(Flow flow) {
      Log.d(TAG, "Flow started: " + flow);
    }

    @Override
    public void onFlowEnd(Flow flow) {
      Log.d(TAG, "Flow ended: " + flow);
      recordDrinkForFlow(flow);
    }
  };

  @Override
  public void onCreate() {
    super.onCreate();
    mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    Log.d(TAG, "onCreate()");
    enableForeground();

    mTapManager = TapManager.getSingletonInstance();
    mFlowManager = FlowManager.getSingletonInstance();
    mFlowManager.addFlowListener(mFlowListener);

    bindToApiService();
    bindToHardwareService();

    mExecutorService = Executors.newSingleThreadExecutor();
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy()");
    mFlowManager.removeFlowListener(mFlowListener);
    if (mApiServiceBound) {
      unbindService(mApiServiceConnection);
      mApiServiceBound = false;
    }
    if (mHardwareServiceBound) {
      unbindService(mHardwareServiceConnection);
      mHardwareServiceBound = false;
    }
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
  private void bindToHardwareService() {
    startService(new Intent(this, KegbotHardwareService.class));
    bindService(new Intent(KegbotCoreService.this, KegbotHardwareService.class),
        mHardwareServiceConnection, Context.BIND_AUTO_CREATE);
    Log.d(TAG, "Bound to hardware service.");
    mHardwareServiceBound = true;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  public void stop() {
    mFlowManager.stop();
    mExecutorService.shutdown();
  }

  private void enableForeground() {
    startForeground(NOTIFICATION_FOREGROUND, buildForegroundNotification());
  }

  private Notification buildForegroundNotification() {
    final Intent intent = new Intent(this, KegtapActivity.class);
    final PendingIntent pendingIntent =
      PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    final Notification.Builder builder = new Notification.Builder(this);
    builder.setOngoing(true)
      .setSmallIcon(R.drawable.icon)
      .setWhen(SystemClock.uptimeMillis())
      .setContentTitle("Kegbot Core is running")
      .setContentIntent(pendingIntent);
    return builder.getNotification();
  }

  /**
   * @param ended
   */
  private void recordDrinkForFlow(Flow ended) {
    Log.d(TAG, "Recording dring for flow: " + ended);
    try {
      Drink d = mApiService.recordDrink(ended.getTap().getMeterName(), ended.getTicks());
      Log.i(TAG, "Recorded drink! " + d);
    } catch (KegbotApiException e) {
      Log.e(TAG, "Error recording drink", e);
    }
  }

  /**
   * @throws KegbotApiException
   *
   */
  private void configure() throws KegbotApiException {
    Log.d(TAG, "Configuring!");
    final String kegbotUrl = PreferenceUtils.getKegbotUrl(mPreferences);
    final Uri apiUrl = KegbotDescriptor.getApiUrl(kegbotUrl);
    mApiService.setApiUrl(apiUrl.toString());

    final String username = PreferenceUtils.getUsername(mPreferences);
    final String password = PreferenceUtils.getPassword(mPreferences);
    mApiService.setAccountCredentials(username, password);

    final TapDetailSet taps = mApiService.getAllTaps();
    Log.d(TAG, "Taps: " + taps);
    for (final Api.TapDetail tapDetail : taps.getTapsList()) {
      Models.KegTap tapInfo = tapDetail.getTap();
      Log.d(TAG, "Adding tap: " + tapInfo.getDescription());
      final Tap tap = new Tap(tapInfo.getDescription(), tapInfo.getMlPerTick(),
          tapInfo.getMeterName(), tapInfo.getRelayName());
      mTapManager.addTap(tap);
    }
  }

  private void debugNotice(String message) {
    Log.d(TAG, message);
    Toast.makeText(KegbotCoreService.this, message, Toast.LENGTH_SHORT).show();
  }

}
