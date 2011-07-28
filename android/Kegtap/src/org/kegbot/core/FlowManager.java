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

  private static final long DEFAULT_IDLE_TIME_MILLIS = TimeUnit.SECONDS.toMillis(30);

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
   * Map of all running flows, keyed by the tap owning the flow.
   */
  private final Map<Tap, Flow> mActiveFlows = Maps.newLinkedHashMap();

  /**
   * Executor that checks for idle flows.
   */
  private final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();

  private final Runnable mIdleChecker = new Runnable() {
    @Override
    public void run() {
      synchronized (mActiveFlows) {
        for (final Flow flow : mActiveFlows.values()) {
          if (flow.isIdle()) {
            Log.d("FlowIdleTask", "Flow is idle, ending: " + flow);
            endFlow(flow);
          }
        }
      }
    }
  };

  private final ScheduledFuture<?> mFuture;

  @VisibleForTesting
  protected FlowManager(final TapManager tapManager) {
    mTapManager = tapManager;
    mFuture = mExecutor.scheduleWithFixedDelay(mIdleChecker, 0, 1000, TimeUnit.MILLISECONDS);
  }

  public void stop() {
    mFuture.cancel(true);
    for (Flow flow : mActiveFlows.values()) {
      endFlow(flow);
    }
  }

  /**
   * Returns all active flows.
   *
   * @return
   */
  public Collection<Flow> getAllActiveFlows() {
    synchronized (mActiveFlows) {
      return ImmutableList.copyOf(mActiveFlows.values());
    }
  }

  /**
   * Returns all flows that are marked as idle.
   *
   * @return
   */
  public Collection<Flow> getIdleFlows() {
    synchronized (mActiveFlows) {
      return ImmutableList.copyOf(Iterables.filter(mActiveFlows.values(), Flow.PREDICATE_IDLE));
    }
  }

  /**
   * Returns all flows that are marked as completed.
   *
   * @return
   */
  public Collection<Flow> getCompletedFlows() {
    synchronized (mActiveFlows) {
      return ImmutableList
          .copyOf(Iterables.filter(mActiveFlows.values(), Flow.PREDICATE_COMPLETED));
    }
  }

  public Flow getFlowForTapName(final String tapName) {
    final Tap tap = mTapManager.getTapForMeterName(tapName);
    if (tap == null) {
      return null;
    }
    final Flow flow = mActiveFlows.get(tap);
    return flow;
  }

  public Flow getFlowForFlowId(final long flowId) {
    for (final Flow flow : mActiveFlows.values()) {
      if (flow.getFlowId() == (int) flowId) {
        return flow;
      }
    }
    return null;
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
    synchronized (mActiveFlows) {
      if (isActivity) {
        flow = getFlowForTapName(tapName);
        if (flow == null) {
          flow = startFlow(tap, DEFAULT_IDLE_TIME_MILLIS);
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
   * Ends the given flow.
   *
   * @param flow
   * @return
   */
  public Flow endFlow(final Flow flow) {
    final Flow removedFlow = mActiveFlows.remove(flow.getTap());
    if (removedFlow == null) {
      return removedFlow;
    }
    removedFlow.setState(State.COMPLETED);
    publishFlowEnd(removedFlow);
    return removedFlow;
  }

  public Flow startFlow(final Tap tap, final long maxIdleTimeMs) {
    Log.d(TAG, "Starting flow on tap " + tap);
    final Flow flow = Flow.build(tap, maxIdleTimeMs);
    synchronized (mActiveFlows) {
      mActiveFlows.put(tap, flow);
      flow.setState(State.ACTIVE);
      flow.pokeActivity();
      publishFlowStart(flow);
    }
    return flow;
  }

  /**
   * Attaches a {@link Listener} to the core.
   *
   * @param listener
   *          the listener
   * @return <code>true</code> if the listener was not already attached
   */
  public boolean addFlowListener(final Listener listener) {
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
      }
    }
  }

  private void publishFlowEnd(final Flow flow) {
    synchronized (mListeners) {
      for (final Listener listener : mListeners) {
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
