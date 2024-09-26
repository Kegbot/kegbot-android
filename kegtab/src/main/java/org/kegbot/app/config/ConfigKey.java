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
package org.kegbot.app.config;

import static org.kegbot.app.config.AppConfiguration.FALSE;
import static org.kegbot.app.config.AppConfiguration.TRUE;

/**
 * Configuration items used by the application. Enum names are used as the key in a backing store.
 */
enum ConfigKey {
  SETUP_VERSION("0"),
  LOCAL_BACKEND(FALSE),

  KEGBOT_URL(""),
  API_KEY(""),
  USERNAME(""),
  PIN(""),

  REGISTRATION_ID(""),

  ALLOW_REGISTRATION(TRUE),
  ALLOW_MANUAL_LOGIN(TRUE),
  CACHE_CREDENTIALS(TRUE),
  RUN_CORE(TRUE),
  ENABLE_AUTOMATIC_FLOW_START(TRUE),

  USE_CAMERA(TRUE),
  AUTO_TAKE_PHOTOS(TRUE),
  TAKE_PHOTOS_DURING_REGISTRATION(FALSE),
  TAKE_PHOTOS_DURING_POUR(TRUE),
  ENABLE_CAMERA_SOUNDS(TRUE),

  FLOW_MINIMUM_VOLUME_ML("10"),
  FLOW_IDLE_TIMEOUT_SECONDS("90"),
  FLOW_IDLE_WARNING_SECONDS("60"),
  ATTRACT_MODE(TRUE),

  UPDATE_REQUIRED(FALSE),
  UPDATE_AVAILABLE(FALSE),

  VOLUME_UNITS_METRIC(FALSE),
  TEMPERATURE_UNITS_CELSIUS(FALSE),

  ABV_DISPLAY_WHEN_ZERO(FALSE),
  IBU_DISPLAY_WHEN_ZERO(FALSE),

  DISPLAY_TAP_NOTES(TRUE),

  STAY_AWAKE(TRUE),
  KEEP_SCREEN_ON(TRUE),
  WAKE_DURING_POUR(FALSE),

  EMAIL_ADDRESS(""),

  LAST_USED_KEG_SIZE(""),

  NETWORK_CONTROLLER_HOST(""),
  NETWORK_CONTROLLER_PORT("8321");


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

