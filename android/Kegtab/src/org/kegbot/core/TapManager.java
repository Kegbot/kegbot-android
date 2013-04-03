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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import org.kegbot.api.KegbotApi;
import org.kegbot.api.KegbotApiException;
import org.kegbot.app.event.DrinkPostedEvent;
import org.kegbot.app.event.TapListUpdateEvent;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.proto.Models.KegTap;

import android.util.Log;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

/**
 * Tap manager.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class TapManager extends Manager {

  private static final String TAG = TapManager.class.getSimpleName();

  private final Map<String, KegTap> mTaps = Maps.newLinkedHashMap();

  /** Normal duration between attempted tap syncs. */
  private static final long SYNC_INTERVAL_NORMAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

  /**
   * Aggressive interval between attempted tap syncs, used when the tap list is
   * empty or when an error is encountered.
   */
  private static final long SYNC_INTERVAL_AGGRESSIVE_MILLIS = TimeUnit.SECONDS.toMillis(10);

  private ScheduledExecutorService mExecutorService;

  private final KegbotApi mApi;

  @GuardedBy("this")
  @Nullable
  private ScheduledFuture<Void> mScheduledSync;

  private final Callable<Void> mTapSyncRunnable = new Callable<Void>() {
    @Override
    public Void call() {
      final List<KegTap> taps;
      try {
        taps = mApi.getAllTaps();
      } catch (KegbotApiException e) {
        Log.w(TAG, "Error fetching taps: " + e, e);
        rescheduleSync(true);
        return null;
      }
      onTapSyncResults(taps);
      rescheduleSync(false);
      return null;
    }
  };

  /**
   * Stores the currently "focused" tap.
   *
   * Convenient bit of state storage for UI layer. May be {@code null}.
   */
  private KegTap mFocusedTap = null;

  public TapManager(Bus bus, KegbotApi api) {
    super(bus);
    mApi = api;
  }

  @Override
  protected synchronized void start() {
    mExecutorService = Executors.newSingleThreadScheduledExecutor();
    mExecutorService.submit(mTapSyncRunnable);
    getBus().register(this);
  }

  @Override
  protected synchronized void stop() {
    getBus().unregister(this);
    mExecutorService.shutdown();
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
  public synchronized boolean removeTap(final KegTap tap) {
    if (mFocusedTap == tap) {
      mFocusedTap = null;
    }
    return mTaps.remove(tap.getMeterName()) != null;
  }

  /**
   * @return the currently-focused tap, or {@code null} if none set
   */
  public synchronized KegTap getFocusedTap() {
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

  public synchronized KegTap getTapForMeterName(final String meterName) {
    for (final KegTap tap : mTaps.values()) {
      if (meterName.equals(tap.getMeterName())) {
        return tap;
      }
    }
    return null;
  }

  public synchronized List<KegTap> getTaps() {
    return Lists.newArrayList(mTaps.values());
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

  private synchronized void onTapSyncResults(List<KegTap> taps) {
    boolean tapsChanged = false;

    Set<String> removedTaps = Sets.newLinkedHashSet(mTaps.keySet());

    for (final KegTap tap : taps) {
      final KegTap existingTap = getTapForMeterName(tap.getMeterName());
      removedTaps.remove(tap.getMeterName());

      if (existingTap == null || !existingTap.equals(tap)) {
        Log.i(TAG, "Adding/updating tap " + tap.getMeterName());
        addTap(tap);
        tapsChanged = true;
      }
    }

    for (String tapName : removedTaps) {
      Log.i(TAG, "Removing tap: " + tapName);
      removeTap(getTapForMeterName(tapName));
      tapsChanged = true;
    }

    if (tapsChanged) {
      TapListUpdateEvent event = new TapListUpdateEvent(Lists.newArrayList(mTaps.values()));
      Log.d(TAG, "Tap set changed, posting event: " + event);
      Log.d(TAG, "Posting..");
      postOnMainThread(event);
      Log.d(TAG, "Event posted.");
    }
  }

  @Produce
  public TapListUpdateEvent produceTapList() {
    return new TapListUpdateEvent(Lists.newArrayList(mTaps.values()));
  }

  @Subscribe
  public void onNewDrinkPosted(DrinkPostedEvent event) {
    syncNow();
  }

  private void syncNow() {
    synchronized (this) {
      if (mScheduledSync != null && !mScheduledSync.isCancelled()) {
        mScheduledSync.cancel(false);
        mScheduledSync = null;
      }
    }
    Log.d(TAG, "Drink posted, syncing now.");
    mExecutorService.submit(mTapSyncRunnable);
  }

  private void rescheduleSync(boolean aggressive) {
    Log.d(TAG, String.format("Rescheduling sync (aggressive=%s)", Boolean.valueOf(aggressive)));
    synchronized (this) {
      mScheduledSync = mExecutorService.schedule(mTapSyncRunnable,
          aggressive ? SYNC_INTERVAL_AGGRESSIVE_MILLIS : SYNC_INTERVAL_NORMAL_MILLIS,
          TimeUnit.MILLISECONDS);
    }
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
      for (final KegTap tap : mTaps.values()) {
        writer.printPair("meterName", tap.getMeterName()).println();
        writer.printPair("mlPerTick", Double.valueOf(tap.getMlPerTick())).println();
        writer.printPair("relayName", tap.getRelayName()).println();
        writer.println();
      }
      writer.decreaseIndent();
    }
  }

}
