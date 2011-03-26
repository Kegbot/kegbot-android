package org.kegbot.kegtap.core;

import org.kegbot.api.KegbotApi;

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

  public KegbotCore(Logger log, KegbotApi api) {
    mLog = log;
    mApi = api;
    mLog.i(TAG, "Kegbot Core starting up.");
  }

  /**
   * Attaches a {@link FlowListener} to the core.
   * 
   * @param listener
   *          the listener
   * @return <code>true</code> if the listener was not already attached
   */
  public boolean addFlowListener(FlowListener listener) {
    // TODO(mikey): implement me
    return false;
  }

  /**
   * Removes a {@link FlowListener} from the core.
   * 
   * @param listener
   *          the listener
   * @return <code>true</code> if the listener was found and removed, false if
   *         the listener was not attached
   */
  public boolean removeFlowListener(FlowListener listener) {
    // TODO(mikey): implement me
    return true;
  }

  /**
   * Main event loop for the core.
   */
  public void run() {
    // TODO(mikey): implement me
  }

}
