package org.kegbot.kegtap.core;

import java.util.Collection;

public interface KegboardHardware {

  /**
   * Returns all flow meter data for sensors managed by this Kegboard.
   * 
   * @return
   */
  public Collection<FlowMeter> getAllFlowMeters();

  /**
   * Returns all temperature sensor data for sensors managed by this Kegboard.
   * 
   * @return
   */
  public Collection<ThermoSensor> getAllThermoSensors();

}
