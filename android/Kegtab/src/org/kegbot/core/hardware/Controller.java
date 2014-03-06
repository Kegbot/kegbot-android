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

  public static final String DEFAULT_DEVICE_NAME = "kegboard";

  /**
   * Returns the current device state.
   */
  public String getStatues();

  /**
   * Returns a uniquely-identifying string name for this device.
   * <p>
   * This name should be stable for the lifetime of the device, ie, derived from
   * its serial number. If the device does not have a unique name, the special
   * value {@link #DEFAULT_DEVICE_NAME} may be returned.
   * </p>
   *
   * @return
   */
  public String getName();

  /** Returns all flow meter ports on this device. */
  public Collection<FlowMeter> getFlowMeters();

  /** Returns a flow meter with the specified name, or {@code null}. */
  public FlowMeter getFlowMeter(final String meterName);

  /** Returns all thermo sensors on this device. */
  public Collection<ThermoSensor> getThermoSensors();

  /** Returns a thermo sensor with the specified name, or {@code null}. */
  public ThermoSensor getThermoSensor(final String sensorName);

}
