/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
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

import com.google.common.base.Strings;
import com.squareup.otto.Bus;

import org.kegbot.app.event.Event;
import org.kegbot.core.AuthenticationToken;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.KegbotCore;
import org.kegbot.core.TapManager;
import org.kegbot.core.hardware.Controller;
import org.kegbot.core.hardware.FakeControllerEvent;
import org.kegbot.core.hardware.FakeControllerManager;
import org.kegbot.core.hardware.MeterUpdateEvent;
import org.kegbot.core.hardware.TokenAttachedEvent;

/**
 * Debugging broadcast receiver. Converts broadcast intents into new {@link Event Events}, which are
 * then posted on the {@link KegbotCore} {@link Bus}.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class DebugBroadcastReceiver extends BroadcastReceiver {

  private static final String TAG = DebugBroadcastReceiver.class.getSimpleName();

  @Override
  public void onReceive(Context context, Intent intent) {
    final String action = intent.getAction();
    Log.d(TAG, "Handling new intent: " + action);

    final KegbotCore core = KegbotCore.getInstance(context);

    if (KegtabBroadcast.ACTION_USER_AUTHED.equals(action)) {
      handleUserAuthed(core, intent);
    } else if (KegtabBroadcast.ACTION_METER_UPDATE.equals(action)) {
      handleMeterUpdate(core, intent);
    } else if (KegtabBroadcast.ACTION_TOKEN_ADDED.equals(action)) {
      handleTokenAdded(core, intent);
    } else if (KegtabBroadcast.ACTION_CONTROLLER_ADDED.equals(action)) {
      handleControllerAddedOrRemoved(core, intent, true);
    } else if (KegtabBroadcast.ACTION_CONTROLLER_REMOVED.equals(action)) {
      handleControllerAddedOrRemoved(core, intent, false);
    } else {
      Log.w(TAG, "Unknown intent action: " + action);
    }
  }

  private void handleUserAuthed(KegbotCore core, Intent intent) {
    Log.d(TAG, "handleUserAuthed: " + intent);

    final String username = intent.getStringExtra(KegtabBroadcast.USER_AUTHED_EXTRA_USERNAME);
    final String tapName = intent.getStringExtra(KegtabBroadcast.DRINKER_SELECT_EXTRA_TAP_NAME);

    final TapManager tapManager = core.getTapManager();

//    if (!Strings.isNullOrEmpty(tapName)) {
//      final KegTap tap = tapManager.getTapForMeterName(tapName);
//      AuthenticatingActivity.startAndAuthenticate(context, username, tap);
//    } else {
//      AuthenticatingActivity.startAndAuthenticate(context, username, (KegTap) null);
//    }
  }

  private void handleMeterUpdate(KegbotCore core, Intent intent) {
    String meterName = intent.getStringExtra(KegtabBroadcast.METER_UPDATE_EXTRA_METER_NAME);
    if (Strings.isNullOrEmpty(meterName)) {
      meterName = "kegboard.flow0";
    }
    final long ticks = intent.getLongExtra(KegtabBroadcast.METER_UPDATE_EXTRA_TICKS, 0);
    if (ticks <= 0) {
      return;
    }

    Log.d(TAG, "Got debug meter update: meter=" + meterName + " ticks=" + ticks);
    final FlowMeter meter = new FlowMeter(meterName);
    meter.setTicks(ticks);
    core.postEvent(new MeterUpdateEvent(meter));
  }

  private void handleTokenAdded(KegbotCore core, Intent intent) {
    final String authDevice = intent.getStringExtra(KegtabBroadcast.TOKEN_ADDED_EXTRA_AUTH_DEVICE);
    final String value = intent.getStringExtra(KegtabBroadcast.TOKEN_ADDED_EXTRA_TOKEN_VALUE);
    if (Strings.isNullOrEmpty(authDevice)) {
      Log.w(TAG, "No auth device in intent.");
      return;
    }
    if (Strings.isNullOrEmpty(value)) {
      Log.w(TAG, "No token value in intent.");
      return;
    }

    Log.d(TAG, "Sending token auth event: authDevice=" + authDevice + " value=" + value);
    final AuthenticationToken token = new AuthenticationToken(authDevice, value);
    core.postEvent(new TokenAttachedEvent(token));
  }

  private void handleControllerAddedOrRemoved(KegbotCore core, Intent intent, boolean added) {
    final String controllerName =
        intent.getStringExtra(KegtabBroadcast.CONTROLLER_ADDED_EXTRA_NAME);
    String status = intent.getStringExtra("status");
    if (status == null) {
      status = Controller.STATUS_OK;
    }
    final String serialNumber = Strings.nullToEmpty(intent.getStringExtra("serial"));

    Log.d(TAG, "Posting controller event added=" + added + " status=" + status);
    final Controller controller = new FakeControllerManager.FakeController(controllerName, status,
        serialNumber);
    core.postEvent(new FakeControllerEvent(controller, added));
  }

}
