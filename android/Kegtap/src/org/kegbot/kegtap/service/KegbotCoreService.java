package org.kegbot.kegtap.service;

import org.kegbot.core.AuthenticationToken;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.ThermoSensor;
import org.kegbot.kegtap.core.AndroidLogger;
import org.kegbot.kegtap.service.KegbotApiService.ConnectionState;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * This service owns and manages a single {@link KegbotCore} instance.
 */
public class KegbotCoreService extends IntentService {

  private static String TAG = KegbotCoreService.class.getSimpleName();

  private static String ACTION_HARDWARE_EVENT = "org.kegbot.kegtap.service.HARDWARE_EVENT";

  private KegbotCore mCore;

  //
  // Connections to other services
  //

  private KegbotApiService mApiService;
  private boolean mApiServiceBound;
  private KegbotHardwareService mHardwareService;
  private boolean mHardwareServiceBound;

  private ServiceConnection mApiServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      mApiService = ((KegbotApiService.LocalBinder) service).getService();
      Log.d(TAG, "!!!!!!!! Api Service connected!");
      // TODO(mikey): WTF@eclipse formatter..
      Toast
      .makeText(KegbotCoreService.this, "Core->API connection established", Toast.LENGTH_SHORT)
      .show();
      attachApiListener();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      mApiService = null;
      Log.d(TAG, "!!!!!!!! Api Service DISCONNECTED!");
      Toast.makeText(KegbotCoreService.this, "Core->API connection lost", Toast.LENGTH_SHORT)
      .show();
    }
  };

  private ServiceConnection mHardwareServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      mHardwareService = ((KegbotHardwareService.LocalBinder) service).getService();
      Log.d(TAG, "!!!!!!!! HW Service connected!");

      Toast.makeText(KegbotCoreService.this, "Core->HW connection established", Toast.LENGTH_SHORT)
      .show();
      attachHardwareListener();
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      mHardwareService = null;
      Log.d(TAG, "!!!!!!!! HW Service DISCONNECTED!");
      Toast.makeText(KegbotCoreService.this, "Core->HW connection lost", Toast.LENGTH_SHORT).show();
    }
  };

  //
  // Local service
  //

  public class LocalBinder extends Binder {
    KegbotCoreService getService() {
      return KegbotCoreService.this;
    }
  }

  private final IBinder mBinder = new LocalBinder();

  //
  // Main methods
  //

  public KegbotCoreService() {
    super(TAG);
    Log.d(TAG, "Starting service.");
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate()");
    bindToApiService();
    bindToHardwareService();
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy()");
    if (mApiServiceBound) {
      unbindService(mApiServiceConnection);
      mApiServiceBound = false;
    }
    if (mHardwareServiceBound) {
      unbindService(mHardwareServiceConnection);
      mHardwareServiceBound = false;
    }
    super.onDestroy();
  }

  /**
   * Attaches to the running {@link KegbotApiService}.
   */
  private void bindToApiService() {
    startService(new Intent(this, KegbotApiService.class));
    bindService(new Intent(KegbotCoreService.this, KegbotApiService.class), mApiServiceConnection,
        Context.BIND_AUTO_CREATE);
    Log.d(TAG, "Bound to api service.");
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

  public KegbotCore getCore() {
    return mCore;
  }

  private void attachApiListener() {
    KegbotApiService.Listener listener = new KegbotApiService.Listener() {
      @Override
      public void onConnectionStateChange(ConnectionState newState) {
        // TODO
      }

      @Override
      public void onConfigurationUpdate() {
        // TODO
      }
    };
    mApiService.attachListener(listener);
  }

  private void attachHardwareListener() {
    KegbotHardwareService.Listener listener = new KegbotHardwareService.Listener() {

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
      }

      @Override
      public void onMeterUpdate(FlowMeter meter) {
      }
    };

    mHardwareService.attachListener(listener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see android.app.IntentService#onHandleIntent(android.content.Intent)
   */
  @Override
  protected void onHandleIntent(Intent intent) {
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    mCore = new KegbotCore(new AndroidLogger(), mApiService, mHardwareService);
    mCore.run();
  }

}
