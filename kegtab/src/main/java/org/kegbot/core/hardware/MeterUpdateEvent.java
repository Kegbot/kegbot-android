package org.kegbot.core.hardware;

import org.kegbot.app.event.Event;
import org.kegbot.core.FlowMeter;

public class MeterUpdateEvent implements Event {

  private final FlowMeter mMeter;

  public MeterUpdateEvent(FlowMeter meter) {
    mMeter = meter;
  }

  public FlowMeter getMeter() {
    return mMeter;
  }

}
