package org.kegbot.core;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.proto.Api;
import org.kegbot.proto.Api.TapDetailSet;
import org.kegbot.proto.Models;
import org.kegbot.proto.Models.Drink;

/**
 * Core logic of the kegbot. Watches sensors, manages flow lifecycles, and
 * records drinks.
 */
public class KegbotCore {

  public static final String TAG = "KegbotCore";

  /**
   * The {@link KegbotApi} instance.
   */
  private final KegbotApi mApi;

  /**
   * A logger, for debug messages.
   */
  private final Logger mLog;

  /**
   * 
   */
  private final KegboardHardware mHw;

  private final FlowManager mFlowManager = new FlowManager();

  public KegbotCore(Logger log, KegbotApi api, KegboardHardware hw) {
    mLog = log;
    mApi = api;
    mHw = hw;
    mLog.i(TAG, "Kegbot Core starting up.");
  }

  /**
   * Main event loop for the core.
   */
  public void run() {
    mLog.i(TAG, "Kegbot core starting up!");

    try {
      configure();
    } catch (KegbotApiException e1) {
      mLog.e(TAG, "Api failed.", e1);
    }

    // Install listener.
    mHw.attachListener(new KegboardHardware.Listener() {

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

    while (true) {

      for (Flow flow : mFlowManager.getIdleFlows()) {
        mLog.d(TAG, "Ending idle flow: " + flow);
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
    mLog.d(TAG, "Recording dring for flow: " + ended);
    try {
      Drink d = mApi.recordDrink(ended.getTap().getMeterName(), ended.getTicks());
      mLog.i(TAG, "Recorded drink! " + d);
    } catch (KegbotApiException e) {
      mLog.e(TAG, "Error recording drink", e);
    }
  }

  /**
   * @throws KegbotApiException
   * 
   */
  private void configure() throws KegbotApiException {
    final TapDetailSet taps = mApi.getAllTaps();
    for (Api.TapDetail tapDetail : taps.getTapsList()) {
      Models.KegTap tapInfo = tapDetail.getTap();
      mLog.d(TAG, "Adding tap: " + tapInfo.getDescription());
      final Tap tap = new Tap(tapInfo.getDescription(), tapInfo.getMlPerTick(),
          tapInfo.getMeterName(), tapInfo.getRelayName());
      mFlowManager.addTap(tap);
    }
  }

}
