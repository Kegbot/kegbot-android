package org.kegbot.core;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kegbot.core.Flow.State;

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

  private long mDefaultIdleTimeMillis = TimeUnit.SECONDS.toMillis(30);

  /**
   * Records the last reading for each tap.
   */
  private final Map<Tap, Integer> mLastTapReading = Maps.newLinkedHashMap();

  /**
   * All flow listeners.
   */
  private Collection<Listener> mListeners = Sets.newLinkedHashSet();

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
  protected FlowManager(final TapManager tapManager) {
    mTapManager = tapManager;
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
  public Collection<Flow> getAllActiveFlows() {
    synchronized (mFlowsByTap) {
      return ImmutableList.copyOf(Iterables.filter(mFlowsByTap.values(), Flow.PREDICATE_ACTIVE));
    }
  }

  /**
   * Returns all flows that are marked as idle.
   *
   * @return
   */
  public Collection<Flow> getIdleFlows() {
    synchronized (mFlowsByTap) {
      return ImmutableList.copyOf(Iterables.filter(mFlowsByTap.values(), Flow.PREDICATE_IDLE));
    }
  }

  /**
   * Returns all flows that are marked as completed.
   *
   * @return
   */
  public Collection<Flow> getCompletedFlows() {
    synchronized (mFlowsByTap) {
      return ImmutableList.copyOf(Iterables.filter(mFlowsByTap.values(), Flow.PREDICATE_COMPLETED));
    }
  }

  public Flow getFlowForTap(final Tap tap) {
    return mFlowsByTap.get(tap);
  }

  public Flow getFlowForMeterName(final String meterName) {
    final Tap tap = mTapManager.getTapForMeterName(meterName);
    if (tap == null) {
      return null;
    }
    return getFlowForTap(tap);
  }

  public Flow getFlowForFlowId(final long flowId) {
    for (final Flow flow : mFlowsByTap.values()) {
      if (flow.getFlowId() == (int) flowId) {
        return flow;
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
    final boolean isActivity;
    if (lastReading == null || lastReading.intValue() > ticks) {
      // First report for this meter.
      delta = 0;
      isActivity = true;
    } else {
      delta = ticks - lastReading.intValue();
      isActivity = (delta > 0);
    }
    mLastTapReading.put(tap, Integer.valueOf(ticks));

    Log.d(TAG, "handleMeterActivity: lastReading=" + lastReading + ", ticks=" + ticks + ", delta="
        + delta);

    Flow flow = null;
    synchronized (mFlowsByTap) {
      if (isActivity) {
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
    }

    return flow;
  }

  /**
   * Handles a user arriving at a tap. If there is no flow in progress, a new
   * flow will be started and this user will be added to it. If there is already
   * a Flow, this user will take it over if anonymous, or end it (and create a
   * new one) otherwise.
   *
   * @param tap
   * @param username
   * @return
   */
  public synchronized Flow activateUserAtTap(final Tap tap, final String username) {
    Flow flow = getFlowForTap(tap);

    if (flow != null && flow.getUsername().equals(username)) {
      Log.d(TAG, "activateUserAtTap: got same username, nothing to do.");
      return flow;
    }

    if (flow != null && flow.isAnonymous()) {
      Log.d(TAG, "activateUserAtTap: existing flow is anonymous, taking it over.");
      flow.setUsername(username);
      publishFlowUpdate(flow);
      return flow;
    } else if (flow != null && flow.isAuthenticated()) {
      Log.d(TAG, "activateUserAtTap: existing flow is authenticated, ending it.");
      endFlow(flow);
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
    final Flow endedFlow = mFlowsByTap.remove(flow.getTap());
    if (endedFlow == null) {
      Log.w(TAG, "No active flow for flow=" + flow + ", tap=" + flow.getTap());
      return endedFlow;
    }
    endedFlow.setState(State.COMPLETED);
    publishFlowEnd(endedFlow);
    if (mFlowsByTap.isEmpty()) {
      stopIdleChecker();
    }
    return endedFlow;
  }

  public Flow startFlow(final Tap tap, final long maxIdleTimeMs) {
    Log.d(TAG, "Starting flow on tap " + tap);
    final Flow flow = Flow.build(tap, maxIdleTimeMs);
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
   * Attaches a {@link Listener} to the core.
   *
   * @param listener
   *          the listener
   * @return <code>true</code> if the listener was not already attached
   */
  public synchronized boolean addFlowListener(final Listener listener) {
    return mListeners.add(listener);
  }

  /**
   * Removes a {@link Listener} from the core.
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
      sSingleton = new FlowManager(TapManager.getSingletonInstance());
    }
    return sSingleton;
  }

}
