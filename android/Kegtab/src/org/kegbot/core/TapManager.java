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

import java.util.Set;

import org.kegbot.app.util.IndentingPrintWriter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Tap manager.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class TapManager extends Manager {

  private static final String TAG = TapManager.class.getSimpleName();

  private final Set<Tap> mTaps = Sets.newLinkedHashSet();

  /**
   * Stores the currently "focused" tap.
   *
   * Convenient bit of state storage for UI layer. May be {@code null}.
   */
  private Tap mFocusedTap = null;

  public synchronized boolean addTap(final Tap newTap) {
    if (mFocusedTap == null) {
      mFocusedTap = newTap;
    }
    return mTaps.add(newTap);
  }

  public synchronized boolean removeTap(final Tap tap) {
    if (mFocusedTap == tap) {
      mFocusedTap = null;
    }
    return mTaps.remove(tap);
  }

  /**
   * @return the currently-focused tap, or {@code null} if none set
   */
  public synchronized Tap getFocusedTap() {
    return mFocusedTap;
  }

  /**
   * Sets the currently focused tap to the tap matching {@code meterName}, or
   * {@code null} if that tap does not exist.
   *
   * @param meterName the name of the focused tap
   */
  public synchronized void setFocusedTap(final String meterName) {
    mFocusedTap = getTapForMeterName(meterName);
  }

  public synchronized Tap getTapForMeterName(final String meterName) {
    for (final Tap tap : mTaps) {
      if (meterName.equals(tap.getMeterName())) {
        return tap;
      }
    }
    return null;
  }

  public synchronized Set<Tap> getTaps() {
    return ImmutableSet.copyOf(mTaps);
  }

  @Override
  protected synchronized void dump(IndentingPrintWriter writer) {
    writer.printf("Tap count: %s\n", Integer.valueOf(mTaps.size()));
    writer.printf("mFocusedTap: %s\n", mFocusedTap);
    writer.println("Taps:");

    writer.increaseIndent();
    for (final Tap tap : mTaps) {
      writer.printPair("meterName", tap.getMeterName());
      writer.printPair("mlPerTick", Double.valueOf(tap.getMlPerTick()));
      writer.printPair("relayName", tap.getRelayName());
      writer.println();
    }
    writer.decreaseIndent();
  }

}
