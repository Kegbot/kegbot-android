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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class TapManager {

  private static TapManager sSingleton = null;

  private final Set<Tap> mTaps = Sets.newLinkedHashSet();

  @VisibleForTesting
  protected TapManager() {
  }

  public synchronized boolean addTap(final Tap newTap) {
    return mTaps.add(newTap);
  }

  public synchronized boolean removeTap(final Tap tap) {
    return mTaps.remove(tap);
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

  public static synchronized TapManager getSingletonInstance() {
    if (sSingleton == null) {
      sSingleton = new TapManager();
    }
    return sSingleton;
  }
}
