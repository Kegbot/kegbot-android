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
