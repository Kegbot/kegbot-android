/**
 *
 */
package org.kegbot.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class PourInProgressReceiver extends BroadcastReceiver {

  private final String TAG = PourInProgressReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    if (KegtapBroadcast.ACTION_POUR_START.equals(intent.getAction())) {
      Log.d(TAG, "Got pour start, starting activity.");
      final String tapName = intent.getStringExtra(KegtapBroadcast.POUR_UPDATE_EXTRA_TAP_NAME);
      final Intent startIntent = PourInProgressActivity.getStartIntent(context, tapName);
      startIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP
          | Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(startIntent);
      abortBroadcast();
    }
  }

}
