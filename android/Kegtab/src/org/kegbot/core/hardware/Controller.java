package org.kegbot.core.hardware;

import org.kegbot.core.FlowMeter;
import org.kegbot.core.ThermoSensor;

import java.util.Collection;

public interface Controller {

  public static final String STATE_DISCONNECTED = "disconnected";
  public static final String STATE_CONNECTING = "connecting";
  public static final String STATE_CONNECTED = "connected";

  /**
   * Returns the current device state.
   *
   *
   * @return one of {@link #STATE_DISCONNECTED}, {@link #STATE_CONNECTING}, or
   *         {@link #STATE_CONNECTED}
   */
  public String getState();

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
