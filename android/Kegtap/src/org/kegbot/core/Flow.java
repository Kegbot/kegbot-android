package org.kegbot.core;

import org.kegbot.proto.Models.User;

import com.google.common.base.Predicate;

public class Flow {

  public static Predicate<Flow> PREDICATE_IDLE = new Predicate<Flow>() {
    @Override
    public boolean apply(Flow flow) {
      return flow.getState() == State.IDLE;
    }
  };

  public static Predicate<Flow> PREDICATE_COMPLETED = new Predicate<Flow>() {
    @Override
    public boolean apply(Flow flow) {
      return flow.getState() == State.COMPLETED;
    }
  };

  public static enum State {
    INITIAL, ACTIVE, IDLE, COMPLETED,
  }

  /**
   * Global for next flow id.
   */
  private static int sFlowId = 0;

  /**
   * Flow id for this instance.
   */
  private final int mFlowId;

  /**
   * Tap for this flow.
   */
  private final Tap mTap;

  /**
   * Authenticated user for this flow. If unset, the flow is anonymous.
   */
  private User mUser;

  /**
   * Current volume record, in milliliters
   */
  private double mVolumeMl = 0.0;

  /**
   * Current volume record, in flow meter ticks.
   */
  private int mTicks = 0;

  /**
   * The flow's state.
   */
  private State mState;

  /**
   * Time the flow was started, in milliseconds since the epoch.
   */
  private long mStartTime;

  /**
   * Time the flow was last updated, or ended, in milliseconds since the epoch.
   */
  private long mUpdateTime;

  /**
   * Maximum idle time, in milliseconds. If zero, flow may remain idle
   * indefinitely.
   */
  private long mMaxIdleTimeMs;

  private Flow(int flowId, Tap tap, long maxIdleTimeMs) {
    mState = State.INITIAL;
    mFlowId = flowId;
    mTap = tap;
    mMaxIdleTimeMs = maxIdleTimeMs;
    mUser = null;
    mTicks = 0;
    mVolumeMl = 0;
    mStartTime = mUpdateTime = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("Flow")
        .append(" id=").append(mFlowId)
        .append(" state=").append(mState)
        .append(" tap=").append(mTap)
        .append(" user=").append(mUser)
        .append(" ticks=").append(mTicks);
    return builder.toString();
  }

  /**
   * Factory method. Generates a new Flow object with a unique flow id.
   *
   * @param tap
   * @param maxIdleTimeMs
   * @return
   */
  public static Flow build(Tap tap, long maxIdleTimeMs) {
    return new Flow(++sFlowId, tap, maxIdleTimeMs);
  }

  /**
   * Sets the flow's last activity time to now.
   */
  public void pokeActivity() {
    mUpdateTime = System.currentTimeMillis();
  }

  /**
   * Increments the flow by the specified number of ticks. The recorded volume
   * is incremented by the correspond volume, as returned by
   * {@link Tap#getVolumeMlForTicks(int)}.
   *
   * @param ticks
   */
  public void addTicks(int ticks) {
    mTicks += ticks;
    mVolumeMl += mTap.getVolumeMlForTicks(ticks);
    pokeActivity();
  }

  /**
   * Increments the flow by the specified volume. The number of recorded ticks
   * is not changed.
   *
   * @param volumeMl
   */
  public void addVolumeMl(double volumeMl) {
    mVolumeMl += volumeMl;
    pokeActivity();
  }

  /**
   * Sets the flow volume to the value given. The number of recorded ticks is
   * not changed.
   *
   * @param volumeMl
   */
  public void setVolumeMl(double volumeMl) {
    mVolumeMl = volumeMl;
    pokeActivity();
  }

  public User getUser() {
    return mUser;
  }

  public void setUser(User user) {
    mUser = user;
  }

  public int getFlowId() {
    return mFlowId;
  }

  public Tap getTap() {
    return mTap;
  }

  public double getVolumeMl() {
    return mVolumeMl;
  }

  public int getTicks() {
    return mTicks;
  }

  public long getMaxIdleTimeMs() {
    return mMaxIdleTimeMs;
  }

  public void setState(State state) {
    mState = state;
  }

  public void setStartTime(long startTime) {
    mStartTime = startTime;
  }

  public void setUpdateTime(long updateTime) {
    mUpdateTime = updateTime;
  }

  public State getState() {
    return mState;
  }

  public long getStartTime() {
    return mStartTime;
  }

  public long getUpdateTime() {
    return mUpdateTime;
  }

  /**
   * Returns true if the flow has exceeded the maximum allowable idle time.
   *
   * @return
   */
  public boolean isIdle() {
    if (mState != State.ACTIVE) {
      return false;
    }
    if (mMaxIdleTimeMs <= 0) {
      return false;
    }
    return (System.currentTimeMillis() - mUpdateTime) > mMaxIdleTimeMs;
  }

  public boolean isAuthenticated() {
    return mUser != null;
  }

}
