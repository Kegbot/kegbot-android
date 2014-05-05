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

import android.util.Log;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.kegbot.app.config.ConfigurationStore;
import org.kegbot.app.event.TapListUpdateEvent;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.proto.Models.KegTap;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tap manager.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class TapManager extends Manager {

  private static final String TAG = TapManager.class.getSimpleName();

  @VisibleForTesting
  protected static final String KEY_HIDDEN_TAP_IDS = "hidden_tap_ids";

  private final Map<Integer, KegTap> mTaps = Maps.newLinkedHashMap();

  private ConfigurationStore mLocalConfig;

  public TapManager(Bus bus, ConfigurationStore configStore) {
    super(bus);
    mLocalConfig = configStore;
  }

  @Override
  protected synchronized void start() {
    getBus().register(this);
  }

  @Override
  protected synchronized void stop() {
    getBus().unregister(this);
    mTaps.clear();
    super.stop();
  }

  /**
   * Adds a Tap to the system.
   *
   * @param newTap
   *          the new tap object
   * @return {@code true} if an existing tap was replaced, {@code false}
   *         otherwise.
   */
  public synchronized boolean addTap(final KegTap newTap) {
    return mTaps.put(Integer.valueOf(newTap.getId()), newTap) != null;
  }

  /**
   * Removes a tap from the system.
   *
   * @param tap
   *          the tap to remove
   * @return {@code true} if an existing tap was removed, {@code false}
   *         otherwise.
   */
  public synchronized boolean removeTap(final KegTap tap) {
    return mTaps.remove(Integer.valueOf(tap.getId())) != null;
  }

  public synchronized KegTap getTap(int tapId) {
    return mTaps.get(Integer.valueOf(tapId));
  }

  @Deprecated
  public synchronized KegTap getTapForMeterName(final String meterName) {
    for (final KegTap tap : mTaps.values()) {
      if (!tap.hasMeter()) {
        continue;
      }
      if (meterName.equals(tap.getMeter().getName())) {
        return tap;
      }
    }
    return null;
  }

  public synchronized List<KegTap> getTaps() {
    return Lists.newArrayList(mTaps.values());
  }

  public synchronized List<KegTap> getVisibleTaps() {
    final Set<String> hiddenIds = mLocalConfig.getStringSet(KEY_HIDDEN_TAP_IDS,
        Collections.<String>emptySet());
    final List<KegTap> results = Lists.newArrayList();

    for (final KegTap tap : mTaps.values()) {
      final String tapId = String.valueOf(tap.getId());
      if (!hiddenIds.contains(tapId)) {
        results.add(tap);
      }
    }
    return results;
  }

  public synchronized Collection<KegTap> getTapsWithActiveKeg() {
    final Set<KegTap> result = Sets.newLinkedHashSet();
    for (final KegTap tap : mTaps.values()) {
      if (tap.hasCurrentKeg()) {
        result.add(tap);
      }
    }
    return result;
  }

  @Subscribe
  public synchronized void onTapSyncResults(TapListUpdateEvent event) {
    final List<KegTap> taps = event.getTaps();
    final Set<Integer> removedTaps = Sets.newLinkedHashSet(mTaps.keySet());

    for (final KegTap tap : taps) {
      final KegTap existingTap = getTap(tap.getId());
      removedTaps.remove(Integer.valueOf(tap.getId()));

      if (existingTap == null || !existingTap.equals(tap)) {
        Log.i(TAG, "Adding/updating tap " + tap.getId());
        addTap(tap);
      }
    }

    for (final Integer tapId : removedTaps) {
      Log.i(TAG, "Removing tap: " + tapId);
      removeTap(getTap(tapId.intValue()));
    }
  }

  public synchronized void setTapVisibility(KegTap tap, boolean isVisible) {
    final String tapId = String.valueOf(tap.getId());
    final Set<String> hiddenTaps = mLocalConfig.getStringSet(KEY_HIDDEN_TAP_IDS,
        Sets.<String>newLinkedHashSet());

    final boolean changed;
    if (isVisible) {
      changed = hiddenTaps.remove(tapId);
    } else {
      changed = hiddenTaps.add(tapId);
    }

    if (changed) {
      mLocalConfig.putStringSet(KEY_HIDDEN_TAP_IDS, hiddenTaps);
    }
  }

  @Override
  protected synchronized void dump(IndentingPrintWriter writer) {
    writer.printPair("numTaps", Integer.valueOf(mTaps.size())).println();

    if (mTaps.size() > 0) {
      writer.println();
      writer.println("All taps:");
      writer.println();
      writer.increaseIndent();
      for (final KegTap tap : mTaps.values()) {
        writer.printPair("tap", tap).println();
        writer.println();
      }
      writer.decreaseIndent();
    }
  }

}
