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
package org.kegbot.core;

import java.util.Date;

public interface Backend {

  /**
   * Records a new drink event.
   *
   * @param tapName
   * @param ticks
   * @param volumeMl
   * @param username
   * @param pourTime
   * @param duration
   * @param authToken
   * @param spilled
   */
  void recordDrink(String tapName, int ticks, float volumeMl, String username, Date pourTime,
      int duration, String authToken, boolean spilled);

  /**
   * Records a new sensor reading.
   *
   * @param sensorName
   * @param temperature
   */
  void logSensorReading(String sensorName, float temperature);

}
