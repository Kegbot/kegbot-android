/**
 *
 */
package org.kegbot.app;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

/**
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class GCMIntentService extends GCMBaseIntentService {

  private static final String TAG = GCMIntentService.class.getSimpleName();

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
    Log.d(TAG, "onError: errorId=" + errorId);
  }

  @Override
  protected void onMessage(Context context, Intent message) {
    Log.d(TAG, "onMessage: message=" + message);
  }

  @Override
  protected void onRegistered(Context context, String regId) {
    Log.d(TAG, "onRegistered: regId=" + regId);
  }

  @Override
  protected void onUnregistered(Context context, String regId) {
    Log.d(TAG, "onUnregistered: regId=" + regId);
  }

}
