package org.kegbot.kegtap.service;

import org.kegbot.api.KegbotApiException;
import org.kegbot.core.AuthenticationToken;
import org.kegbot.core.Flow;
import org.kegbot.core.FlowManager;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.Tap;
import org.kegbot.core.ThermoSensor;
import org.kegbot.kegtap.service.KegbotApiService.ConnectionState;
import org.kegbot.proto.Api;
import org.kegbot.proto.Api.TapDetailSet;
import org.kegbot.proto.Models;
import org.kegbot.proto.Models.Drink;

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

  private final FlowManager mFlowManager = new FlowManager();

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

  private void attachApiListener() {
    mApiService.attachListener(new KegbotApiService.Listener() {
      @Override
      public void onConnectionStateChange(ConnectionState newState) {
        // TODO
      }

      @Override
      public void onConfigurationUpdate() {
        // TODO
      }
    });
  }

  private void attachHardwareListener() {
    mHardwareService.attachListener(new KegbotHardwareService.Listener() {

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
        mFlowManager.handleMeterActivity(meter.getName(), (int) meter.getTicks());
      }
    });
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
    run();
  }

  /**
   * Main event loop for the core.
   */
  public void run() {
    Log.i(TAG, "Kegbot core starting up!");

    try {
      configure();
    } catch (KegbotApiException e1) {
      Log.e(TAG, "Api failed.", e1);
    }

    while (true) {

      for (Flow flow : mFlowManager.getIdleFlows()) {
        Log.d(TAG, "Ending idle flow: " + flow);
        mFlowManager.endFlow(flow);
        recordDrinkForFlow(flow);
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  public void stop() {
    mFlowManager.stop();
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
    final TapDetailSet taps = mApiService.getAllTaps();
    for (Api.TapDetail tapDetail : taps.getTapsList()) {
      Models.KegTap tapInfo = tapDetail.getTap();
      Log.d(TAG, "Adding tap: " + tapInfo.getDescription());
      final Tap tap = new Tap(tapInfo.getDescription(), tapInfo.getMlPerTick(),
          tapInfo.getMeterName(), tapInfo.getRelayName());
      mFlowManager.addTap(tap);
    }
  }

}
