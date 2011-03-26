package org.kegbot.kegtap.core;

/**
 * Represents a physical keg tap.
 */
public class Tap {

  /**
   * Name of this tap.
   */
  private String mName;

  /**
   * Number of milliliters per flow meter tick. May be zero.
   */
  private double mMlPerTick = 0.0;

  /**
   * Name of the flow meter backing this tap.
   */
  private String mMeterName;

  /**
   * Name of the relay backing this tap, if any.
   */
  private String mRelayName;

  Tap(String name, float mlPerTick, String meterName, String relayName) {
    mName = name;
    mMlPerTick = mlPerTick;
    mMeterName = meterName;
    mRelayName = relayName;
  }

  public double getVolumeMlForTicks(int ticks) {
    return ticks * mMlPerTick;
  }

}
