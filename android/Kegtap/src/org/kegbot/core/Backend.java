package org.kegbot.core;

import java.util.Date;

public interface Backend {
  
  void recordDrink(String tapName, int ticks, float volumeMl, String username, Date pourTime, int duration, String authToken, boolean spilled);
  
  void logSensorReading(String sensorName, float temperature);

}
