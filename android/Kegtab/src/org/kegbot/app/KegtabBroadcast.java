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

import org.kegbot.core.AuthenticationToken;
import org.kegbot.core.Flow;

import android.content.Intent;

import com.google.common.base.Strings;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegtabBroadcast {

  /**
   * Action set when a pour is being updated.
   */
  public static final String ACTION_POUR_UPDATE = "org.kegbot.action.POUR_UPDATE";

  /**
   * Action set when a pour is being updated.
   */
  public static final String ACTION_POUR_START = "org.kegbot.action.POUR_START";

  /**
   * Extra to be sent with {@link #ACTION_POUR_UPDATE}, giving the flow id as a
   * long.
   */
  public static final String POUR_UPDATE_EXTRA_TAP_NAME = "tap";

  /**
   * Action set when a new temperature reading is recorded.
   */
  public static final String ACTION_THERMO_UPDATE = "org.kegbot.action.THERMO_UPDATE";
  public static final String THERMO_UPDATE_EXTRA_SENSOR_NAME = "sensor_name";
  public static final String THERMO_UPDATE_EXTRA_TEMP_C = "temp_c";

  public static final String ACTION_USER_AUTHED = "org.kegbot.action.USER_AUTHED";
  public static final String USER_AUTHED_EXTRA_USERNAME = "username";
  public static final String USER_AUTHED_EXTRA_TAP_NAME = "tap_name";

  public static final String ACTION_PICTURE_TAKEN = "org.kegbot.action.PICTURE_TAKEN";
  public static final String PICTURE_TAKEN_EXTRA_FILENAME = "filename";

  public static final String ACTION_PICTURE_DISCARDED = "org.kegbot.action.PICTURE_DISCARDED";
  public static final String PICTURE_DISCARDED_EXTRA_FILENAME = "filename";

  public static final String DRINKER_SELECT_EXTRA_TAP_NAME = "tap";

  public static final String ACTION_AUTH_BEGIN = "org.kegbot.action.AUTH_BEGIN";
  public static final String ACTION_AUTH_FAIL = "org.kegbot.action.AUTH_FAIL";
  public static final String AUTH_FAIL_EXTRA_MESSAGE = "message";

  public static final String ACTION_TOKEN_ADDED = "org.kegbot.action.TOKEN_ADDED";
  public static final String TOKEN_ADDED_EXTRA_AUTH_DEVICE = "auth_device";
  public static final String TOKEN_ADDED_EXTRA_TOKEN_VALUE = "token";

  /**
   * Non-instantiable.
   */
  private KegtabBroadcast() {
    assert (false);
  }

  public static Intent getPourStartBroadcastIntent(final Flow flow) {
    final Intent intent = new Intent(ACTION_POUR_START);
    intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
    //intent.putExtra(POUR_UPDATE_EXTRA_TAP_NAME, flow.getTap().getMeterName());
    return intent;
  }

  public static Intent getPourUpdateBroadcastIntent(final Flow flow) {
    final Intent intent = new Intent(ACTION_POUR_UPDATE);
    intent.putExtra(POUR_UPDATE_EXTRA_TAP_NAME, flow.getTap().getMeterName());
    return intent;
  }

  public static Intent getThermoUpdateBroadcastIntent(final String sensorName, final double tempC) {
    final Intent intent = new Intent(ACTION_THERMO_UPDATE);
    intent.putExtra(THERMO_UPDATE_EXTRA_SENSOR_NAME, sensorName);
    intent.putExtra(THERMO_UPDATE_EXTRA_TEMP_C, tempC);
    return intent;
  }

  public static Intent getUserAuthedBroadcastIntent(final String username) {
    final Intent intent = new Intent(ACTION_USER_AUTHED);
    intent.putExtra(USER_AUTHED_EXTRA_USERNAME, username);
    return intent;
  }

  public static Intent getPictureTakenBroadcastIntent(final String filename) {
    final Intent intent = new Intent(ACTION_PICTURE_TAKEN);
    intent.putExtra(PICTURE_TAKEN_EXTRA_FILENAME, filename);
    return intent;
  }

  public static Intent getPictureDiscardedBroadcastIntent(final String filename) {
    final Intent intent = new Intent(ACTION_PICTURE_DISCARDED);
    intent.putExtra(PICTURE_DISCARDED_EXTRA_FILENAME, filename);
    return intent;
  }

  public static Intent getAuthBeginIntent(AuthenticationToken token) {
    final Intent intent = new Intent(ACTION_AUTH_BEGIN);
    return intent;
  }

  public static Intent getAuthFailIntent(String message) {
    final Intent intent = new Intent(ACTION_AUTH_FAIL);
    if (!Strings.isNullOrEmpty(message)) {
      intent.putExtra(AUTH_FAIL_EXTRA_MESSAGE, message);
    }
    return intent;
  }

  public static Intent getTokenAddedIntent(String authDevice, String tokenValue) {
    final Intent intent = new Intent(ACTION_TOKEN_ADDED);
    intent.putExtra(TOKEN_ADDED_EXTRA_AUTH_DEVICE, authDevice);
    intent.putExtra(TOKEN_ADDED_EXTRA_TOKEN_VALUE, tokenValue);
    return intent;
  }

}
