/*
 * Copyright 2013 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For more
 * information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.app.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Represents a time series, a vector of (time, value) pairs.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class TimeSeries {

  /** Immutable bean representing a single data point. */
  public static final class Point {
    public final long time;
    public final long value;

    public Point(long time, long value) {
      this.time = time;
      this.value = value;
    }

    @Override
    public String toString() {
      return String.format("(%s, %s)", Long.valueOf(time), Long.valueOf(value));
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (time ^ (time >>> 32));
      result = prime * result + (int) (value ^ (value >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Point other = (Point) obj;
      return time == other.time && value == other.value;
    }

  }

  /** Internal immutable list of points. */
  private final List<Point> mPoints;

  public static TimeSeries.Builder newBuilder(int maxResolution, boolean rebaseTimes) {
    return new TimeSeries.Builder(maxResolution, rebaseTimes);
  }

  public static TimeSeries fromString(String s) {
    Builder b = TimeSeries.newBuilder(0, false);
    for (String pairString : Splitter.onPattern("\\s+").split(s)) {
      String[] items = pairString.split(":");
      if (items.length != 2) {
        throw new IllegalArgumentException("Pair did not contain exactly two items.");
      }
      long time;
      try {
        time = Long.valueOf(items[0]).longValue();
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Error parsing time: " + e, e);
      }

      long value;
      try {
        value = Long.valueOf(items[1]).longValue();
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Error parsing value: " + e, e);
      }

      b.add(time, value);
    }
    return b.build();
  }

  private TimeSeries(List<Point> points) {
    mPoints = ImmutableList.copyOf(points);
  }

  public List<Point> getPoints() {
    return mPoints;
  }

  @Override
  public String toString() {
    return asString();
  }

  public String asString() {
    StringBuilder builder = new StringBuilder();
    boolean firstPoint = true;
    for (final Point point : mPoints) {
      if (!firstPoint) {
        builder.append(' ');
      }
      firstPoint = false;
      builder.append(point.time);
      builder.append(':');
      builder.append(point.value);
    }
    return builder.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + mPoints.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TimeSeries other = (TimeSeries) obj;
    return mPoints.equals(other.mPoints);
  }

  public static class Builder {
    private final long mMinResolution;
    private final boolean mRebaseTimes;
    private final List<Point> mPoints = Lists.newArrayList();
    private long mBaseTime = 0;

    /**
     * Constructor.
     *
     * @param minResolution
     *          minimum time delta between data points. Adjacent points will be
     *          coalesced until a point is added whose {@code time} exceeds the
     *          last value by at least this much. The value {@code 0} disables
     *          coalescing.
     * @param rebaseTimes
     *          if {@code true}, all timestamps will be relative to the first
     *          data point's time, which will be stored as {@code 0}.
     */
    public Builder(long minResolution, boolean rebaseTimes) {
      mMinResolution = minResolution;
      mRebaseTimes = rebaseTimes;
    }

    /**
     * Adds a new data point.  {@code time} values must be added in increasing order.
     *
     * @param time the event time
     * @param value the event value
     * @return {@code this}, for chaining
     */
    public Builder add(long time, long value) {
      // Add immediately if no reason to coalesce.
      if (mPoints.isEmpty()) {
        if (mRebaseTimes) {
          mPoints.add(new Point(0, value));
          mBaseTime = time;
        } else {
          mPoints.add(new Point(time, value));
        }
        return this;
      }

      if (mRebaseTimes) {
        time -= mBaseTime;
      }

      final int lastLocation = mPoints.size() - 1;
      final Point lastPoint = mPoints.get(lastLocation);
      if (time == lastPoint.time || mMinResolution > 0 && (lastPoint.time + mMinResolution) > time) {
        // Last point is recent; coalesce.
        mPoints.remove(lastLocation);
        mPoints.add(new Point(lastPoint.time, lastPoint.value + value));
      } else {
        mPoints.add(new Point(time, value));
      }

      return this;
    }

    public int size() {
      return mPoints.size();
    }

    public TimeSeries build() {
      return new TimeSeries(mPoints);
    }

  }

}
