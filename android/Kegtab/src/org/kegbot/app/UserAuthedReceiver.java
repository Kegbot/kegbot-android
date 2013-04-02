/*

 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app;

import org.kegbot.core.FlowManager;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.TapManager;
import org.kegbot.proto.Models.KegTap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.common.base.Strings;

/**
 * Debugging broadcast receiver.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class UserAuthedReceiver extends BroadcastReceiver {

  private static final String TAG = UserAuthedReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    final String action = intent.getAction();
    handleAuthBegin(context, intent);

    if (KegtabBroadcast.ACTION_USER_AUTHED.equals(action)) {
      handleUserAuthed(context, intent);
    }
  }

  private void handleAuthBegin(Context context, Intent intent) {
    final Intent activityIntent = new Intent(intent);
    activityIntent.setClass(context, AuthenticatingActivity.class);
    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    context.startActivity(activityIntent);
  }

  private void handleUserAuthed(Context context, Intent intent) {
    Log.d(TAG, "handleUserAuthed: " + intent);

    final String username = intent.getStringExtra(KegtabBroadcast.USER_AUTHED_EXTRA_USERNAME);
    final String tapName = intent.getStringExtra(KegtabBroadcast.DRINKER_SELECT_EXTRA_TAP_NAME);

    final FlowManager flowManager = KegbotCore.getInstance(context).getFlowManager();
    final TapManager tapManager = KegbotCore.getInstance(context).getTapManager();

    if (!Strings.isNullOrEmpty(tapName)) {
      final KegTap tap = tapManager.getTapForMeterName(tapName);
      flowManager.activateUserAtTap(tap, username);
    } else {
      flowManager.activateUserAmbiguousTap(username);
    }
  }

}
