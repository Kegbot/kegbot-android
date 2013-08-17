/*
 * Copyright 2013 Mike Wakerly <opensource@hoho.com>.
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
package org.kegbot.app.config;

import static org.kegbot.app.config.AppConfiguration.FALSE;
import static org.kegbot.app.config.AppConfiguration.TRUE;

/**
 * Configuration items used by the application. Enum names are used as the key
 * in a backing store.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
enum ConfigKey {
  SETUP_VERSION("0"),

  KEGBOT_URL(""),
  API_KEY(""),
  USERNAME(""),
  PIN(""),

  IS_REGISTERED(TRUE),
  ALLOW_REGISTRATION(TRUE),
  ALLOW_MANUAL_LOGIN(TRUE),
  CACHE_CREDENTIALS(TRUE),
  RUN_CORE(TRUE),
  ENABLE_AUTOMATIC_FLOW_START(TRUE),

  FLOW_MINIMUM_VOLUME_ML("10"),
  FLOW_IDLE_TIMEOUT_SECONDS("90"),
  FLOW_IDLE_WARNING_SECONDS("60"),
  AUTO_TAKE_PHOTOS(TRUE),
  ATTRACT_MODE(TRUE),

  GCM_REGISTRATION_ID(""),

  LAST_CHECKIN_ATTEMPT_MILLIS(String.valueOf(Long.MIN_VALUE)),
  LAST_CHECKIN_SUCCESS_MILLIS(String.valueOf(Long.MIN_VALUE)),
  LAST_CHECKIN_STATUS(""),
  UPDATE_REQUIRED(FALSE),
  UPDATE_NEEDED(FALSE),

  VOLUME_UNITS_METRIC(FALSE),
  TEMPERATURE_UNITS_CELSIUS(FALSE),

  KEGBOARD_NAME("kegboard"),
  STAY_AWAKE(TRUE),
  KEEP_SCREEN_ON(TRUE),
  ;

  private final String mDefaultValue;

  ConfigKey(String defaultValue) {
    mDefaultValue = defaultValue;
  }

  /**
   * Returns the key name that should be used.
   *
   * @return the key name
   */
  String getName() {
    return name();
  }

  String getDefault() {
    return mDefaultValue;
  }

}

