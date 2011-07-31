/**
 *
 */
package org.kegbot.kegtap;

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
      final long flowId = intent.getLongExtra(KegtapBroadcast.POUR_UPDATE_EXTRA_FLOW_ID, -1);
      final Intent startIntent = PourInProgressActivity.getStartIntent(context, flowId);
      startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
      context.startActivity(startIntent);
      abortBroadcast();
    }
  }

}
