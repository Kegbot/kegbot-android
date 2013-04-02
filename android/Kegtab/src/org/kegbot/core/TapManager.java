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

import java.util.Map;
import java.util.Set;

import org.kegbot.api.KegbotApi;
import org.kegbot.app.util.IndentingPrintWriter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.squareup.otto.Bus;

/**
 * Tap manager.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class TapManager extends Manager {

  private static final String TAG = TapManager.class.getSimpleName();

  private final Map<String, Tap> mTaps = Maps.newLinkedHashMap();

  private final KegbotApi mApi;

  /**
   * Stores the currently "focused" tap.
   *
   * Convenient bit of state storage for UI layer. May be {@code null}.
   */
  private Tap mFocusedTap = null;

  public TapManager(Bus bus, KegbotApi api) {
    super(bus);
    mApi = api;
  }

  /**
   * Adds a Tap to the system.
   *
   * @param newTap
   *          the new tap object
   * @return {@code true} if an existing tap was replaced, {@code false}
   *         otherwise.
   */
  public synchronized boolean addTap(final Tap newTap) {
    if (mFocusedTap == null) {
      mFocusedTap = newTap;
    }
    return mTaps.put(newTap.getMeterName(), newTap) != null;
  }

  /**
   * Removes a tap from the system.
   *
   * @param tap
   *          the tap to remove
   * @return {@code true} if an existing tap was removed, {@code false}
   *         otherwise.
   */
  public synchronized boolean removeTap(final Tap tap) {
    if (mFocusedTap == tap) {
      mFocusedTap = null;
    }
    return mTaps.remove(tap.getMeterName()) != null;
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
    for (final Tap tap : mTaps.values()) {
      if (meterName.equals(tap.getMeterName())) {
        return tap;
      }
    }
    return null;
  }

  public synchronized Set<Tap> getTaps() {
    return ImmutableSet.copyOf(mTaps.values());
  }

  @Override
  protected synchronized void dump(IndentingPrintWriter writer) {
    writer.printPair("numTaps", Integer.valueOf(mTaps.size())).println();
    writer.printPair("mFocusedTap", mFocusedTap).println();

    if (mTaps.size() > 0) {
      writer.println();
      writer.println("All taps:");
      writer.println();
      writer.increaseIndent();
      for (final Tap tap : mTaps.values()) {
        writer.printPair("meterName", tap.getMeterName()).println();
        writer.printPair("mlPerTick", Double.valueOf(tap.getMlPerTick())).println();
        writer.printPair("relayName", tap.getRelayName()).println();
        writer.println();
      }
      writer.decreaseIndent();
    }
  }

}
