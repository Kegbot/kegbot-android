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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.http.annotation.GuardedBy;
import org.kegbot.core.Flow.State;

import android.os.SystemClock;
import android.util.Log;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class FlowManager {

  private static final String TAG = FlowManager.class.getSimpleName();

  private static FlowManager sSingleton = null;

  private final TapManager mTapManager;

  private final Clock mClock;

  private int mNextFlowId = 1;

  private long mDefaultIdleTimeMillis = TimeUnit.SECONDS.toMillis(30);

  /**
   * Records the last reading for each tap.
   */
  private final Map<Tap, Integer> mLastTapReading = Maps.newLinkedHashMap();

  /**
   * All flow listeners.
   */
  @GuardedBy("mListeners")
  private Collection<Listener> mListeners = Sets.newLinkedHashSet();

  public interface Clock {
    public long currentTimeMillis();
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
     * Called when a flow has been finished by the core, that is, when the
     * flow's state has changed to {@link Flow.State#COMPLETED}.
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
  private final Map<Tap, Flow> mFlowsByTap = Maps.newLinkedHashMap();

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

  @VisibleForTesting
  protected FlowManager(final TapManager tapManager, final Clock clock) {
    mTapManager = tapManager;
    mClock = clock;
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

  public void stop() {
    stopIdleChecker();
    for (Flow flow : getAllActiveFlows()) {
      endFlow(flow);
    }
  }

  /**
   * Returns all active flows.
   *
   * @return
   */
  public List<Flow> getAllActiveFlows() {
    synchronized (mFlowsByTap) {
      return ImmutableList.copyOf(Iterables.filter(mFlowsByTap.values(), Flow.PREDICATE_ACTIVE));
    }
  }

  /**
   * Returns all flows that are marked as idle.
   *
   * @return
   */
  public List<Flow> getIdleFlows() {
    synchronized (mFlowsByTap) {
      return ImmutableList.copyOf(Iterables.filter(mFlowsByTap.values(), Flow.PREDICATE_IDLE));
    }
  }

  public Flow getFlowForTap(final Tap tap) {
    synchronized (mFlowsByTap) {
      return mFlowsByTap.get(tap);
    }
  }

  public Flow getFlowForMeterName(final String meterName) {
    final Tap tap = mTapManager.getTapForMeterName(meterName);
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

  public void setDefaultIdleTimeMillis(long defaultIdleTimeMillis) {
    mDefaultIdleTimeMillis = defaultIdleTimeMillis;
  }

  public long getDefaultIdleTimeMillis() {
    return mDefaultIdleTimeMillis;
  }

  public Flow handleMeterActivity(final String tapName, final int ticks) {
    Log.d(TAG, "handleMeterActivity: " + tapName + "=" + ticks);
    final Tap tap = mTapManager.getTapForMeterName(tapName);
    if (tap == null || tapName == null) {
      Log.d(TAG, "Dropping activity for unknown tap: " + tapName);
      return null;
    }

    final Integer lastReading = mLastTapReading.get(tap);
    int delta;
    if (lastReading == null || lastReading.intValue() > ticks) {
      // First report for this meter.
      delta = 0;
    } else {
      delta = Math.max(0, ticks - lastReading.intValue());
    }

    mLastTapReading.put(tap, Integer.valueOf(ticks));

    Log.d(TAG, "handleMeterActivity: lastReading=" + lastReading + ", ticks=" + ticks + ", delta="
        + delta);

    Flow flow = null;
    synchronized (mFlowsByTap) {
      flow = getFlowForTap(tap);
      if (flow == null || flow.getState() != Flow.State.ACTIVE) {
        flow = startFlow(tap, mDefaultIdleTimeMillis);
        Log.d(TAG, "  started new flow: " + flow);
      } else {
        Log.d(TAG, "  found existing flow: " + flow);
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
   * @return
   */
  public synchronized Flow activateUserAtTap(final Tap tap, final String username) {
    Flow flow = getFlowForTap(tap);
    Log.d(TAG, "Activating username=" + username + " at tap=" + tap + " current flow=" + flow);

    if (flow != null) {
      if (!flow.isAuthenticated()) {
        Log.d(TAG, "activateUserAtTap: existing flow is anonymous, taking it over.");
        flow.setUsername(username);
        publishFlowUpdate(flow);
        return flow;
      } else {
        if (flow.getUsername().equals(username)) {
          Log.d(TAG, "activateUserAtTap: got same username, nothing to do.");
          return flow;
        } else {
          Log.d(TAG, "activateUserAtTap: existing flow is for different user; replacing.");
          endFlow(flow);
        }
      }
    }

    // New flow to replace previous or empty.
    Log.d(TAG, "activateUserAtTap: creating new flow.");
    flow = startFlow(tap, mDefaultIdleTimeMillis);
    flow.setUsername(username);
    publishFlowUpdate(flow);
    return flow;
  }

  /**
   * Ends the given flow.
   *
   * @param flow
   * @return
   */
  public Flow endFlow(final Flow flow) {
    final Flow endedFlow;
    synchronized (mFlowsByTap) {
      endedFlow = mFlowsByTap.remove(flow.getTap());
      if (mFlowsByTap.isEmpty()) {
        stopIdleChecker();
      }
    }
    if (endedFlow == null) {
      Log.w(TAG, "No active flow for flow=" + flow + ", tap=" + flow.getTap());
      return endedFlow;
    }
    endedFlow.setState(State.COMPLETED);
    publishFlowEnd(endedFlow);

    return endedFlow;
  }

  public Flow startFlow(final Tap tap, final long maxIdleTimeMs) {
    Log.d(TAG, "Starting flow on tap " + tap);
    final Flow flow = new Flow(mClock, mNextFlowId++, tap, maxIdleTimeMs);
    synchronized (mFlowsByTap) {
      mFlowsByTap.put(tap, flow);
      flow.setState(State.ACTIVE);
      flow.pokeActivity();
      publishFlowStart(flow);
    }
    startIdleChecker();
    return flow;
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

  public static synchronized FlowManager getSingletonInstance() {
    if (sSingleton == null) {
      final Clock clock = new Clock() {
        @Override
        public long currentTimeMillis() {
          return SystemClock.uptimeMillis();
        }
      };
      sSingleton = new FlowManager(TapManager.getSingletonInstance(), clock);
    }
    return sSingleton;
  }

}
