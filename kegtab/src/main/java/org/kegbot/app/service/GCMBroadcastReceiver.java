/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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
