package org.kegbot.core;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.kegbot.core.Flow.State;

import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class FlowManager {

  private static final String TAG = FlowManager.class.getSimpleName();

  private final Map<String, Tap> mTaps = Maps.newLinkedHashMap();

  private final Map<Tap, Integer> mLastTapReading = Maps.newLinkedHashMap();

  private Collection<Listener> mListeners = Sets.newLinkedHashSet();

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

  private final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();

  private final Runnable mIdleChecker = new FlowIdleTask();
  private final ScheduledFuture<?> mFuture;

  public FlowManager() {
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

  public Tap getTapForName(String tapName) {
    return mTaps.get(tapName);
  }

  public Flow getFlowForTapName(String tapName) {
    final Tap tap = mTaps.get(tapName);
    if (tap == null) {
      return null;
    }
    final Flow flow = mActiveFlows.get(tap);
    return flow;
  }

  public boolean addTap(Tap tap) {
    final String tapName = tap.getMeterName();
    Log.d(TAG, "Installing new tap: name=" + tapName);

    if (mTaps.get(tapName) != null) {
      return false;
    }
    mTaps.put(tapName, tap);
    mLastTapReading.put(tap, Integer.valueOf(-1));
    return true;
  }

  public Tap removeTap(String tapName) {
    final Tap tap = mTaps.remove(tapName);
    if (tap != null) {
      mLastTapReading.remove(tap);
    }
    return tap;
  }

  public Flow handleMeterActivity(String tapName, int ticks) {
    final Tap tap = mTaps.get(tapName);
    if (tapName == null) {
      Log.d(TAG, "Dropping activity for unknown tap: " + tapName);
      return null;
    }
    final int lastTicks = mLastTapReading.get(tap).intValue();

    final int delta;
    if (lastTicks >= 0) {
      delta = ticks - lastTicks;
    } else {
      delta = 0;
    }

    Log.d(TAG, "handleMeterActivity: lastTicks=" + lastTicks + ", ticks=" + ticks + ", delta="
        + delta);

    Flow flow = null;
    synchronized (mActiveFlows) {
      if (delta > 0 || lastTicks < 0) {
        flow = getFlowForTapName(tapName);
        if (flow == null) {
          flow = startFlow(tap, 5000);
          Log.d(TAG, "  started new flow: " + flow);
        } else {
          Log.d(TAG, "  found existing flow: " + flow);
        }
        flow.addTicks(delta);
        publishFlowUpdate(flow);
      }
    }

    mLastTapReading.put(tap, Integer.valueOf(ticks));
    return flow;
  }

  /**
   * Ends the given flow.
   * 
   * @param flow
   * @return
   */
  public Flow endFlow(Flow flow) {
    final Flow removedFlow = mActiveFlows.remove(flow.getTap());
    if (removedFlow == null) {
      return removedFlow;
    }
    removedFlow.setState(State.COMPLETED);
    publishFlowEnd(removedFlow);
    return removedFlow;
  }

  public Flow startFlow(Tap tap, int maxIdleTimeMs) {
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
  public boolean addFlowListener(Listener listener) {
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
  public boolean removeFlowListener(Listener listener) {
    return mListeners.remove(listener);
  }

  private void publishFlowUpdate(Flow flow) {
    for (final Listener listener : mListeners) {
      listener.onFlowUpdate(flow);
    }
  }

  private void publishFlowStart(Flow flow) {
    for (final Listener listener : mListeners) {
      listener.onFlowStart(flow);
    }
  }

  private void publishFlowEnd(Flow flow) {
    for (final Listener listener : mListeners) {
      listener.onFlowEnd(flow);
    }
  }

  private class FlowIdleTask implements Runnable {

    @Override
    public void run() {
      synchronized (mActiveFlows) {
        for (Flow flow : mActiveFlows.values()) {
          if (flow.isIdle()) {
            Log.d("FlowIdleTask", "Marking flow as idle: " + flow);
            flow.setState(State.IDLE);
            publishFlowUpdate(flow);
          }
        }
      }
    }

  }

}
