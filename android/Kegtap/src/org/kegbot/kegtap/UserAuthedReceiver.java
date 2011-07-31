/**
 *
 */
package org.kegbot.kegtap;

import org.kegbot.core.FlowManager;
import org.kegbot.core.Tap;
import org.kegbot.core.TapManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class UserAuthedReceiver extends BroadcastReceiver {

  private static final String TAG = UserAuthedReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    final String action = intent.getAction();
    if (KegtapBroadcast.ACTION_USER_AUTHED.equals(action)) {
      handleUserAuthed(context, intent);
    }
  }

  private void handleUserAuthed(Context context, Intent intent) {
    Log.d(TAG, "handldeUserAuthed: " + intent);
    final String username = intent.getStringExtra(KegtapBroadcast.USER_AUTHED_EXTRA_USERNAME);

    FlowManager flowManager = FlowManager.getSingletonInstance();
    TapManager tapManager = TapManager.getSingletonInstance();

    for (Tap tap : tapManager.getTaps()) {
      flowManager.activateUserAtTap(tap, username);
    }
  }

}
