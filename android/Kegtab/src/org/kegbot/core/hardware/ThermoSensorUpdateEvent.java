package org.kegbot.core.hardware;

import org.kegbot.app.event.Event;
import org.kegbot.core.ThermoSensor;

public class ThermoSensorUpdateEvent implements Event {

  private final ThermoSensor mSensor;

  public ThermoSensorUpdateEvent(ThermoSensor sensor) {
    mSensor = sensor;
  }

  public ThermoSensor getSensor() {
    return mSensor;
  }

}
