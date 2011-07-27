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
    if (PourInProgressActivity.ACTION_POUR_UPDATE.equals(intent.getAction())) {
      Log.d(TAG, "Got pour update, starting activity.");
      final long flowId = intent.getLongExtra(PourInProgressActivity.EXTRA_FLOW_ID, -1);
      final Intent startIntent = PourInProgressActivity.getStartIntent(context, flowId);
      startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(startIntent);
      abortBroadcast();
    }
  }

}
