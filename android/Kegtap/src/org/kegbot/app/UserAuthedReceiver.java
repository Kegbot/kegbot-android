/**
 *
 */
package org.kegbot.app;

import org.kegbot.core.FlowManager;
import org.kegbot.core.Tap;
import org.kegbot.core.TapManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.common.base.Strings;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class UserAuthedReceiver extends BroadcastReceiver {

  private static final String TAG = UserAuthedReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    final String action = intent.getAction();
    if (KegtapBroadcast.ACTION_AUTH_BEGIN.equals(action)
        || KegtapBroadcast.ACTION_AUTH_FAIL.equals(action)) {
      handleAuthBegin(context, intent);
    } else if (KegtapBroadcast.ACTION_USER_AUTHED.equals(action)) {
      handleUserAuthed(context, intent);
    }
  }

  private void handleAuthBegin(Context context, Intent intent) {
    final Intent activityIntent = new Intent(intent);
    activityIntent.setClass(context, AuthenticatingActivity.class);
    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    context.startActivity(activityIntent);
  }

  private void handleUserAuthed(Context context, Intent intent) {
    Log.d(TAG, "handleUserAuthed: " + intent);
    final String username = intent.getStringExtra(KegtapBroadcast.USER_AUTHED_EXTRA_USERNAME);
    final String tapName = intent.getStringExtra(KegtapBroadcast.DRINKER_SELECT_EXTRA_TAP_NAME);

    FlowManager flowManager = FlowManager.getSingletonInstance();
    TapManager tapManager = TapManager.getSingletonInstance();
    if (!Strings.isNullOrEmpty(tapName)) {
      final Tap tap = tapManager.getTapForMeterName(tapName);
      flowManager.activateUserAtTap(tap, username);
    } else {
      for (final Tap tap : tapManager.getTaps()) {
        flowManager.activateUserAtTap(tap, username);
      }
    }
  }

}
