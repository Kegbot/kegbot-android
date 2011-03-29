package org.kegbot.core;

/**
 * Represents a physical keg tap.
 */
public class Tap {

  /**
   * Name of this tap.
   */
  private final String mName;

  /**
   * Number of milliliters per flow meter tick. May be zero.
   */
  private double mMlPerTick = 0.0;

  /**
   * Name of the flow meter backing this tap.
   */
  private final String mMeterName;

  /**
   * Name of the relay backing this tap, if any.
   */
  private final String mRelayName;

  /**
   * Constructs a new tap instance.
   * 
   * @param name
   *          the name of the tap
   * @param mlPerTick
   *          number of milliliters per flow meter tick
   * @param meterName
   *          meter name, which should match the meter's name in the kegbot
   *          backend
   * @param relayName
   *          relay name, if any
   */
  Tap(String name, float mlPerTick, String meterName, String relayName) {
    mName = name;
    mMlPerTick = mlPerTick;
    mMeterName = meterName;
    mRelayName = relayName;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("[Tap").append(" meterName=").append(mMeterName)
        .append(" name=").append(mName).append("]");
    return builder.toString();
  }

  /**
   * Returns the volume corresponding to the number of ticks given.
   * 
   * @param ticks
   * @return
   */
  public double getVolumeMlForTicks(int ticks) {
    return ticks * mMlPerTick;
  }

  public double getMlPerTick() {
    return mMlPerTick;
  }

  public void setMlPerTick(double mlPerTick) {
    mMlPerTick = mlPerTick;
  }

  public String getName() {
    return mName;
  }

  public String getMeterName() {
    return mMeterName;
  }

  public String getRelayName() {
    return mRelayName;
  }

}
