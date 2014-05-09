/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.core;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.kegbot.app.util.TimeSeries;
import org.kegbot.core.FlowManager.Clock;
import org.kegbot.proto.Models.KegTap;

import java.util.List;

import javax.annotation.Nullable;

/**
 * A flow holds all state about an active pour in progress.
 */
public class Flow {

  /** Clock instance, used for updating timekeeping. */
  private Clock mClock;

  /** All flows are reported on a meter. */
  private final String mMeterName;

  /** Flow id for this instance. */
  private final int mFlowId;

  /** Tap for this flow; may be null if tap was unknown at time of flow. */
  @Nullable
  private final KegTap mTap;

  /** Authenticated user for this flow. If unset, the flow is anonymous. */
  private String mUsername;

  /** Current volume record, in flow meter ticks. */
  private int mTicks = 0;

  /** Time the flow was started, in {@link Clock#elapsedRealtime()}. */
  private final long mStartTimeMillis;

  /**
   * Time the flow was ended, in {@link Clock#elapsedRealtime()}.
   * <p/>
   * Only meaningful when {@link #mIsFinished} is {@code true}.
   */
  private long mEndTimeMillis;

  /** Time of the last call to {@link #addTicks(int)}. */
  private long mLastUpdateTimeMillis;

  /**
   * Last "activity" time. This usually matches {@link #mLastUpdateTimeMillis}, but may be reset in
   * {@link #pokeActivity()}.
   */
  private long mLastActivityTimeMillis;

  /**
   * Maximum idle time, in milliseconds. If zero, flow may remain idle indefinitely.
   */
  private long mMaxIdleTimeMillis;

  /** Optional message added to the flow by the user. */
  private String mShout = "";

  /** Set to {@code true} when the flow is finished. */
  private boolean mIsFinished = false;

  /** Image files attached to this flow. */
  private final List<String> mImages = Lists.newArrayList();

  private final TimeSeries.Builder mTimeSeries = TimeSeries.newBuilder(100, true);

  public Flow(Clock clock, String meterName, int flowId, KegTap tap, long maxIdleTimeMs) {
    mClock = clock;
    mMeterName = meterName;
    mFlowId = flowId;
    mTap = tap;
    mMaxIdleTimeMillis = maxIdleTimeMs;
    mUsername = "";
    mTicks = 0;
    mStartTimeMillis = clock.elapsedRealtime();
    mLastUpdateTimeMillis = clock.elapsedRealtime();
    mLastActivityTimeMillis = mLastUpdateTimeMillis;
    mTimeSeries.add(clock.elapsedRealtime(), 0);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder("Flow")
        .append(" id=").append(mFlowId)
        .append(" meterName=").append(mMeterName)
        .append(" finished=").append(mIsFinished)
        .append(" user=").append(mUsername)
        .append(" ticks=").append(getTicks())
        .append(" volume_ml=").append(getVolumeMl());
    if (mTap != null) {
      builder.append(" tap=").append(mTap.getId());
    }
    if (!mImages.isEmpty()) {
      builder.append(" numImages=").append(mImages.size());
    }
    if (!Strings.isNullOrEmpty(mShout)) {
      builder.append(" shout='").append(mShout).append("'");
    }
    return builder.toString();
  }

  /** Resets the flow's idle time. */
  public void pokeActivity() {
    mLastActivityTimeMillis = mClock.elapsedRealtime();
  }

  /**
   * Increments the flow by the specified number of ticks.
   *
   * @param ticks number of ticks to add
   */
  public void addTicks(int ticks) {
    Preconditions.checkState(!mIsFinished, "Flow is already finished, cannot add ticks.");
    mTicks += ticks;

    long now = mClock.elapsedRealtime();
    mLastUpdateTimeMillis = now;
    mLastActivityTimeMillis = now;
    mTimeSeries.add(now, ticks);
  }

  public String getMeterName() {
    return mMeterName;
  }

  public String getUsername() {
    return mUsername;
  }

  public void setUsername(String username) {
    Preconditions.checkState(!mIsFinished, "Flow is already finished, cannot set username.");
    mUsername = username;
  }

  public int getFlowId() {
    return mFlowId;
  }

  public KegTap getTap() {
    return mTap;
  }

  public double getVolumeMl() {
    if (mTap == null) {
      return 0;
    }
    final double factor = mTap.getMeter().getTicksPerMl();
    if (factor <= 0) {
      return 0;
    }
    return mTicks / factor;
  }

  public int getTicks() {
    return mTicks;
  }

  public long getMaxIdleTimeMs() {
    return mMaxIdleTimeMillis;
  }

  public void setFinished() {
    Preconditions.checkState(!mIsFinished, "Flow is already finished, cannot finish again.");
    mIsFinished = true;
    mEndTimeMillis = mClock.elapsedRealtime();
    mTimeSeries.add(mEndTimeMillis, 0);
  }

  public boolean isFinished() {
    return mIsFinished;
  }

  public long getDurationMs() {
    if (!mIsFinished) {
      return mClock.elapsedRealtime() - mStartTimeMillis;
    }
    return mEndTimeMillis - mStartTimeMillis;
  }

  public long getIdleTimeMs() {
    return mClock.elapsedRealtime() - mLastActivityTimeMillis;
  }

  public long getMsUntilIdle() {
    return Math.max(mMaxIdleTimeMillis - getIdleTimeMs(), 0);
  }

  public void addImage(String image) {
    mImages.add(image);
  }

  public boolean removeImage(String image) {
    return mImages.remove(image);
  }

  public List<String> getImages() {
    return ImmutableList.copyOf(mImages);
  }

  public void setShout(String shout) {
    if (shout == null) {
      mShout = "";
    } else {
      mShout = shout;
    }
  }

  public String getShout() {
    return mShout;
  }

  public TimeSeries getTickTimeSeries() {
    return mTimeSeries.build();
  }

  /**
   * Returns true if the flow has exceeded the maximum allowable idle time.
   *
   * @return
   */
  public boolean isIdle() {
    if (mMaxIdleTimeMillis <= 0) {
      return false;
    }
    return getIdleTimeMs() >= mMaxIdleTimeMillis;
  }

  public boolean isAuthenticated() {
    return !Strings.isNullOrEmpty(mUsername);
  }

  public boolean isAnonymous() {
    return !isAuthenticated();
  }

}
