/**
 *
 */
package org.kegbot.app.service;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.common.base.Strings;

/**
 * Handles the GCM intent.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class GCMBroadcastReceiver extends BroadcastReceiver {

  private static final String TAG = GCMBroadcastReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    final String command = intent.getStringExtra("command");
    Log.d(TAG, "Received GCM broadcast, command=" + command);

    if ("checkin".equals(command)) {
      CheckinService.requestImmediateCheckin(context);
    } else if ("broadcast".equals(command)) {
      final String action = intent.getStringExtra("broadcast_action");
      if (Strings.isNullOrEmpty(action)) {
        Log.w(TAG, "Broadcast intent with no action, ignoring.");
      } else {
        Log.d(TAG, "Broadcast action=" + action);
        final Intent newIntent = new Intent(action);
        intent.putExtras(intent.getExtras());
        context.sendBroadcast(newIntent);
      }
    } else {
      Log.d(TAG, "Ignoring unknown command.");
    }

    setResultCode(Activity.RESULT_OK);
  }

}
