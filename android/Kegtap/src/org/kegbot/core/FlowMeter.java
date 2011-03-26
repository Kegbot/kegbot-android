package org.kegbot.core;

/**
 * Holds the state of a flow meter input managed by a kegboard.
 */
public class FlowMeter {

  private final String mName;

  private long mTicks;

  FlowMeter(String name) {
    mName = name;
    mTicks = 0;
  }

  public long getTicks() {
    return mTicks;
  }

  public void setTicks(long ticks) {
    mTicks = ticks;
  }

  public String getName() {
    return mName;
  }

}
