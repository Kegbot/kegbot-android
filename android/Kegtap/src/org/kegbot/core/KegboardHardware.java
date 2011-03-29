package org.kegbot.core;

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

  public boolean attachListener(Listener listener);

  public boolean removeListener(Listener listener);

  /**
   * Receives notifications for hardware events.
   */
  public interface Listener {

    /**
     * Notifies the listener that a flow meter's state has changed.
     * 
     * @param meter
     *          the meter that was updated
     */
    public void onMeterUpdate(FlowMeter meter);

    /**
     * Notifies the listener that a thermo sensor's state has changed.
     * 
     * @param sensor
     *          the sensor that was updated
     */
    public void onThermoSensorUpdate(ThermoSensor sensor);

    /**
     * An authentication token was momentarily swiped.
     * 
     * @param token
     * @param tapName
     */
    public void onTokenSwiped(AuthenticationToken token, String tapName);

    /**
     * A token was attached.
     * 
     * @param token
     * @param tapName
     */
    public void onTokenAttached(AuthenticationToken token, String tapName);

    /**
     * A token was removed.
     * 
     * @param token
     * @param tapName
     */
    public void onTokenRemoved(AuthenticationToken token, String tapName);

  }

}
