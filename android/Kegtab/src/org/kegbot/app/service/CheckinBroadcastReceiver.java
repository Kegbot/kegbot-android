/**
 *
 */
package org.kegbot.app.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Starts the checkin service.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class CheckinBroadcastReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    final String action = intent.getAction();
    if (CheckinService.CHECKIN_NOW_ACTION.equals(action)) {
      CheckinService.startCheckinService(context, true);
    }
  }

}
