/**
 *
 */
package org.kegbot.app;

import org.kegbot.app.service.CheckinService;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

/**
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class GCMIntentService extends GCMBaseIntentService {

  private static final String LOG_TAG = GCMIntentService.class.getSimpleName();

  private static final String GCM_ACTION_CHECKIN = "checkin";

  public GCMIntentService() {
    super("GCMIntentService");
  }

  /**
   * @param senderId
   */
  protected GCMIntentService(String senderId) {
    super(senderId);
  }

  @Override
  protected void onError(Context context, String errorId) {
    Log.d(LOG_TAG, "onError: errorId=" + errorId);
  }

  @Override
  protected void onMessage(Context context, Intent message) {
    Log.d(LOG_TAG, "onMessage: message=" + message);
    final String action = message.getStringExtra("action");
    Log.d(LOG_TAG, "action=" + action);
    if (GCM_ACTION_CHECKIN.equals(action)) {
      Log.d(LOG_TAG, "Immediate checkin requested.");
      CheckinService.requestImmediateCheckin(context);
    } else {
      Log.d(LOG_TAG, "Unknown action.");
    }
  }

  @Override
  protected void onRegistered(Context context, String regId) {
    Log.d(LOG_TAG, "onRegistered: regId=" + regId);
  }

  @Override
  protected void onUnregistered(Context context, String regId) {
    Log.d(LOG_TAG, "onUnregistered: regId=" + regId);
  }

}
