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

/**
 * Holds the state of a temperature sensor managed by a kegboard.
 */
public class ThermoSensor {

  /**
   * Constant reported when there is no valid temperature.
   */
  public final static double TEMPERATURE_INVALID = Double.NaN;

  /**
   * The name of the sensor, as reported by the kegboard.
   */
  private final String mName;

  /**
   * Last reported temperature, in degrees C.
   */
  private double mTemperatureC;

  public ThermoSensor(String name) {
    mName = name;
    mTemperatureC = TEMPERATURE_INVALID;
  }

  /**
   * Returns the last recorded temperature, or {@link #TEMPERATURE_INVALID} if
   * there is not a valid record.
   *
   * @return
   */
  public double getTemperatureC() {
    return mTemperatureC;
  }

  /**
   * Sets the recorded temperature.
   *
   * @param temperatureC
   */
  public void setTemperatureC(double temperatureC) {
    mTemperatureC = temperatureC;
  }

  public String getName() {
    return mName;
  }

}
