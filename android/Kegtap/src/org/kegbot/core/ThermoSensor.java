package org.kegbot.core;

/**
 * Holds the state of a temperature sensor managed by a kegboard.
 */
class ThermoSensor {

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

  ThermoSensor(String name) {
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
