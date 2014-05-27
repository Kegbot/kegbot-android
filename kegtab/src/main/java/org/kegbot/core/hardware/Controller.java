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

package org.kegbot.core.hardware;

import org.kegbot.core.FlowMeter;
import org.kegbot.core.ThermoSensor;

import java.util.Collection;

public interface Controller {

  /** Default status; undefined. */
  public static final String STATUS_UNKNOWN = "unknown";

  /** Controller is online and operating normally. */
  public static final String STATUS_OK = "ok";

  /** Controller disabled: requires a firmware update. */
  public static final String STATUS_NEED_UPDATE = "need-update";

  /** Controller disabled: requires a serial number. */
  public static final String STATUS_NEED_SERIAL_NUMBER = "need-serial-number";

  /** Controller disabled: stopped responding. */
  public static final String STATUS_UNRESPONSIVE = "unresponsive";

  /** Controller disabled: error opening the device. */
  public static final String STATUS_OPEN_ERROR = "open-error";

  /** Controller disabled: a Controller with the same name already exists. */
  public static final String STATUS_NAME_CONFLICT = "name-conflict";

  /** @see #getName() */
  public static final String DEFAULT_DEVICE_NAME = "kegboard";

  /** @see #getDeviceType() */
  public static final String TYPE_UNKNOWN = "Unknown";

  /** @see #getDeviceType() */
  public static final String TYPE_KBPM = "Kegboard Pro Mini";

  /**
   * Returns the current device state.
   */
  public String getStatus();

  /**
   * Returns a uniquely-identifying string name for this device. <p> This name should be stable for
   * the lifetime of the device, ie, derived from its serial number. If the device does not have a
   * unique name, the special value {@link #DEFAULT_DEVICE_NAME} may be returned. </p>
   *
   * @return
   */
  public String getName();

  /** Returns the device's unique serial number. */
  public String getSerialNumber();

  /** One of {@link #TYPE_UNKNOWN} or {@link #TYPE_KBPM}. */
  public String getDeviceType();

  /** Returns all flow meter ports on this device. */
  public Collection<FlowMeter> getFlowMeters();

  /** Returns a flow meter with the specified name, or {@code null}. */
  public FlowMeter getFlowMeter(final String meterName);

  /** Returns all thermo sensors on this device. */
  public Collection<ThermoSensor> getThermoSensors();

  /** Returns a thermo sensor with the specified name, or {@code null}. */
  public ThermoSensor getThermoSensor(final String sensorName);

}
