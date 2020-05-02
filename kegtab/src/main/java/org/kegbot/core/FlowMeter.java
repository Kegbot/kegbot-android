/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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


/**
 * Holds the state of a flow meter input managed by a kegboard.
 */
public class FlowMeter {

  private final String mMeterName;

  private long mTicks;

  public FlowMeter(final String meterName) {
    mMeterName = meterName;
    mTicks = 0;
  }

  public long getTicks() {
    return mTicks;
  }

  public void setTicks(long ticks) {
    mTicks = ticks;
  }

  public String getMeterName() {
    return mMeterName;
  }

}
