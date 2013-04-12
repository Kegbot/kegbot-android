/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
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
package org.kegbot.core;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.proto.Models.KegTap;

import android.util.Log;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.squareup.otto.Bus;

public class FlowManager extends Manager {

  private static final String TAG = FlowManager.class.getSimpleName();

  private static final int MAX_RECENT_FLOWS = 10;

  public static Predicate<Flow> PREDICATE_IDLE = new Predicate<Flow>() {
    @Override
    public boolean apply(Flow flow) {
      return flow.isIdle();
    }
  };

  private Function<Flow, KegTap> FLOW_TO_METER_NAME = new Function<Flow, KegTap>() {
    @Override
    public KegTap apply(Flow flow) {
      return mTapManager.getTapForMeterName(flow.getTap().getMeterName());
    }
  };

  private final TapManager mTapManager;
  private final AppConfiguration mConfig;
  private final Clock mClock;

  /** Cache of current and recent flows, for debugging. */
  private final Deque<Flow> mRecentFlows = new ArrayDeque<Flow>(MAX_RECENT_FLOWS);

  private int mNextFlowId = 1;

  /**
   * Records the last reading for each tap, by meter name.
   */
  private final Map<String, Integer> mLastTapReading = Maps.newLinkedHashMap();

  /**
   * All flow listeners.
   */
  //@GuardedBy("mListeners")
  private Collection<Listener> mListeners = Sets.newLinkedHashSet();

  public interface Clock {

    /**
     * Returns a strictly increasing monotonic time, such as {@link android.os.SystemClock#elapsedRealtime()}
     *
     * @return
     */
    public long elapsedRealtime();
  }

  /**
   * Listener interfaces for updates to managed flows.
   */
  public interface Listener {

    /**
     * Called when a new flow has been started by the core.
     *
     * @param flow
     *          the new flow
     */
    public void onFlowStart(Flow flow);

    /**
     * Called when a flow has been finished.
     *
     * @param flow
     */
    public void onFlowEnd(Flow flow);

    /**
     * Called when a flow has been updated.
     *
     * @param flow
     */
    public void onFlowUpdate(Flow flow);

  }

  /**
   * Map of all flows, keyed by the tap owning the flow.
   */
  private final Map<String, Flow> mFlowsByTap = Maps.newLinkedHashMap();

  /**
   * Executor that checks for idle flows.
   */
  private final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();

  private final Runnable mIdleChecker = new Runnable() {
    @Override
    public void run() {
      for (final Flow flow : getIdleFlows()) {
        Log.d("FlowIdleTask", "Flow is idle, ending: " + flow);
        try {
          endFlow(flow);
        } catch (Exception e) {
          Log.wtf("FlowIdleTask", "UNCAUGHT EXCEPTION", e);
        }
      }
    }
  };

  private ScheduledFuture<?> mFuture;

  FlowManager(final Bus bus, final TapManager tapManager, final AppConfiguration preferences,
      final Clock clock) {
    super(bus);
    mTapManager = tapManager;
    mClock = clock;
    mConfig = preferences;
  }

  private synchronized void startIdleChecker() {
    if (mFuture == null) {
      Log.d(TAG, "Starting idle checker.");
      mFuture = mExecutor.scheduleWithFixedDelay(mIdleChecker, 0, 1000, TimeUnit.MILLISECONDS);
    }
  }

  private synchronized void stopIdleChecker() {
    if (mFuture != null) {
      Log.d(TAG, "Stopping idle checker.");
      mFuture.cancel(true);
      mFuture = null;
    }
  }

  @Override
  protected void stop() {
    stopIdleChecker();
    for (Flow flow : getAllActiveFlows()) {
      endFlow(flow);
    }
    super.stop();
  }

  /**
   * Returns all active flows.
   *
   * @return
   */
  public List<Flow> getAllActiveFlows() {
    synchronized (mFlowsByTap) {
      return ImmutableList.copyOf(mFlowsByTap.values());
    }
  }

  /** @return all taps with an active {@link Flow}.*/
  public List<KegTap> getAllActiveTaps() {
    return Lists.newArrayList(Collections2.transform(getAllActiveFlows(), FLOW_TO_METER_NAME));
  }

  /**
   * Returns all flows that are marked as idle.
   *
   * @return
   */
  public List<Flow> getIdleFlows() {
    synchronized (mFlowsByTap) {
      return ImmutableList.copyOf(Iterables.filter(mFlowsByTap.values(), PREDICATE_IDLE));
    }
  }

  @VisibleForTesting
  protected Flow getFlowForTap(final KegTap tap) {
    synchronized (mFlowsByTap) {
      return mFlowsByTap.get(tap.getMeterName());
    }
  }

  public Flow getFlowForMeterName(final String meterName) {
    final KegTap tap = mTapManager.getTapForMeterName(meterName);
    if (tap == null) {
      return null;
    }
    return getFlowForTap(tap);
  }

  public Flow getFlowForFlowId(final long flowId) {
    synchronized (mFlowsByTap) {
      for (final Flow flow : mFlowsByTap.values()) {
        if (flow.getFlowId() == (int) flowId) {
          return flow;
        }
      }
    }
    return null;
  }

  public Flow handleMeterActivity(final String tapName, final int ticks) {
    final KegTap tap = mTapManager.getTapForMeterName(tapName);
    if (tap == null || tapName == null) {
      Log.d(TAG, String.format("handleMeterActivity: tap=%s ticks=%s: unknown tap, dropping.",
          tapName, Integer.valueOf(ticks)));
      return null;
    }

    final Integer lastReading = mLastTapReading.get(tap.getMeterName());
    int delta;
    if (lastReading == null || lastReading.intValue() > ticks) {
      // First report for this meter.
      delta = 0;
    } else {
      delta = Math.max(0, ticks - lastReading.intValue());
    }

    mLastTapReading.put(tap.getMeterName(), Integer.valueOf(ticks));

    Log.d(TAG, "handleMeterActivity: lastReading=" + lastReading + ", ticks=" + ticks + ", delta="
        + delta);

    Log.d(TAG, String.format("handleMeterActivity: tap=%s ticks=%s last=%s delta=%s",
        tapName, Integer.valueOf(ticks), Integer.valueOf(ticks), Integer.valueOf(delta)));

    Flow flow = null;
    synchronized (mFlowsByTap) {
      flow = getFlowForTap(tap);
      if (flow == null) {
        if (!mConfig.getEnableFlowAutoStart()) {
          Log.d(TAG, "  ! not starting new flow, autostart disabled.");
          return null;
        }
        flow = startFlow(tap, mConfig.getIdleTimeoutMs());
        Log.d(TAG, "  + started new flow: " + flow);
      } else {
        Log.d(TAG, "  ~ found existing flow: " + flow);
      }
      flow.addTicks(delta);
      publishFlowUpdate(flow);
    }

    return flow;
  }

  /**
   * Handles a user arriving at a tap. If there is no flow in progress, a new
   * flow will be started and this user will be added to it. If there is already
   * a Flow, this user will take it over if anonymous. A non-anonymous active
   * flow is left untouched.
   *
   * @param tap
   * @param username
   */
  public synchronized void activateUserAtTap(final KegTap tap, final String username) {
    Flow flow = getFlowForTap(tap);
    Log.d(TAG, "Activating username=" + username + " at tap=" + tap.getMeterName()
        + " current flow=" + flow);

    if (flow != null) {
      // Already a flow at the tap.
      if (!flow.isAuthenticated()) {
        Log.d(TAG, "activateUserAtTap: existing flow is anonymous, taking it over.");
        flow.setUsername(username);
        publishFlowUpdate(flow);
        return;
      } else {
        if (flow.getUsername().equals(username)) {
          Log.d(TAG, "activateUserAtTap: got same username, nothing to do.");
          return;
        } else {
          Log.d(TAG, "activateUserAtTap: existing flow is for different user; replacing.");
          endFlow(flow);
        }
      }
    }

    // New flow to replace previous or empty.
    Log.d(TAG, "activateUserAtTap: creating new flow.");
    flow = startFlow(tap, mConfig.getIdleTimeoutMs());
    flow.setUsername(username);
    publishFlowUpdate(flow);
  }

  /**
   * Like {@link #activateUserAtTap(KegTap, String)}, but used when the desired tap
   * for activation is unknown.
   *
   * This method arises since some authentication sources (eg RFID tag) are not
   * bound to a particular tap. When a drinker authenticates with such a source,
   * we must decide which taps to activate.
   *
   * @param username
   */
  public synchronized void activateUserAmbiguousTap(final String username) {
    final Collection<KegTap> availableTaps = mTapManager.getTapsWithActiveKeg();

    if (availableTaps.isEmpty()) {
      Log.w(TAG, "activateUserAmbiguousTap: No active taps!");
      // TODO(mikey): raise error.
      return;
    }

    final Set<KegTap> activateTaps = Sets.newLinkedHashSet();

    final KegTap focusedTap = mTapManager.getFocusedTap();
    if (focusedTap != null && focusedTap.hasCurrentKeg()) {
      Log.d(TAG, String.format("activateUserAmbiguousTap: using focused tap: %s", focusedTap));
      activateTaps.add(focusedTap);
    }

    for (final KegTap tap : availableTaps) {
      if (tap == focusedTap) {
        continue;
      }
      if (Strings.isNullOrEmpty(tap.getRelayName())) {
        if (tap.hasCurrentKeg()) {
          Log.d(TAG, String.format("activateUserAmbiguousTap: also activating at unmanaged tap: %s",
              focusedTap));
          activateTaps.add(tap);
        }
      }
    }

    if (activateTaps.isEmpty()) {
      Log.d(TAG, "activateUserAmbiguousTap: no focused or unmanaged taps; activating on all taps");
      activateTaps.addAll(availableTaps);
    }

    for (final KegTap tap : activateTaps) {
      activateUserAtTap(tap, username);
    }
  }

  /**
   * Ends the given flow.
   *
   * @param flow
   * @return the ended flow.
   */
  public Flow endFlow(final Flow flow) {
    final Flow endedFlow;
    synchronized (mFlowsByTap) {
      endedFlow = mFlowsByTap.remove(flow.getTap().getMeterName());
      if (mFlowsByTap.isEmpty()) {
        stopIdleChecker();
      }
    }
    if (endedFlow == null) {
      Log.w(TAG, "No active flow for flow=" + flow + ", tap=" + flow.getTap());
      return endedFlow;
    }
    endedFlow.setFinished();
    publishFlowEnd(endedFlow);
    Log.d(TAG, String.format("Ended flow: id=%s ticks=[%s]",
        Integer.valueOf(flow.getFlowId()), flow.getTickTimeSeries().asString()));

    return endedFlow;
  }

  public Flow startFlow(final KegTap tap, final long maxIdleTimeMs) {
    Log.d(TAG, "Starting flow on tap " + tap.getMeterName());
    final Flow flow = new Flow(mClock, mNextFlowId++, tap, maxIdleTimeMs);
    mRecentFlows.addLast(flow);
    if (mRecentFlows.size() > MAX_RECENT_FLOWS) {
      mRecentFlows.removeFirst();
    }
    synchronized (mFlowsByTap) {
      mFlowsByTap.put(tap.getMeterName(), flow);
      flow.pokeActivity();
      publishFlowStart(flow);
    }
    startIdleChecker();
    return flow;
  }

  @Override
  protected void dump(IndentingPrintWriter writer) {
    List<Flow> activeFlows = getAllActiveFlows();
    writer.printPair("numActiveFlows", Integer.valueOf(activeFlows.size()))
        .println();
    writer.printPair("totalFlowsProcessed", Integer.valueOf(mNextFlowId - 1))
        .println();
    writer.println();

    if (!activeFlows.isEmpty()) {
      writer.println("Active flows:");
      writer.println();
      writer.increaseIndent();
      for (final Flow flow : activeFlows) {
        dumpFlow(writer, flow);
      }
      writer.decreaseIndent();
      writer.println();
    }

    List<Flow> recentFlows = Lists.newArrayList(mRecentFlows);
    if (!recentFlows.isEmpty()) {
      writer.println("Recent flows:");
      writer.println();
      writer.increaseIndent();
      for (final Flow flow : recentFlows) {
        if (!flow.isFinished()) {
          continue;
        }
        dumpFlow(writer, flow);
      }
      writer.decreaseIndent();
      writer.println();
    }
  }

  private static void dumpFlow(IndentingPrintWriter writer, Flow flow) {
    writer.printPair("id", Integer.valueOf(flow.getFlowId()))
        .println();
    writer.printPair("ticks", Integer.valueOf(flow.getTicks()))
        .println();
    writer.printPair("strval", flow.toString())
        .println();
    writer.printPair("timeSeries", flow.getTickTimeSeries().asString())
        .println();
    writer.println();

  }

  /**
   * Attaches a {@link Listener} to the flow manager.
   *
   * @param listener
   *          the listener
   * @return <code>true</code> if the listener was not already attached
   */
  public boolean addFlowListener(final Listener listener) {
    synchronized (mListeners) {
      return mListeners.add(listener);
    }
  }

  /**
   * Removes a {@link Listener} from the flow manager.
   *
   * @param listener
   *          the listener
   * @return <code>true</code> if the listener was found and removed, false if
   *         the listener was not attached
   */
  public boolean removeFlowListener(final Listener listener) {
    synchronized (mListeners) {
      return mListeners.remove(listener);
    }
  }

  private void publishFlowUpdate(final Flow flow) {
    synchronized (mListeners) {
      for (final Listener listener : mListeners) {
        listener.onFlowUpdate(flow);
      }
    }
  }

  private void publishFlowStart(final Flow flow) {
    synchronized (mListeners) {
      for (final Listener listener : mListeners) {
        listener.onFlowStart(flow);
        listener.onFlowUpdate(flow);
      }
    }
  }

  private void publishFlowEnd(final Flow flow) {
    synchronized (mListeners) {
      for (final Listener listener : mListeners) {
        listener.onFlowUpdate(flow);
        listener.onFlowEnd(flow);
      }
    }
  }

}
