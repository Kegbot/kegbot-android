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
