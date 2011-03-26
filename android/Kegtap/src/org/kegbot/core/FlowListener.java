package org.kegbot.core;

public interface FlowListener {

  /**
   * Called when a new flow has been started by the core.
   * 
   * @param flow
   *          the new flow
   */
  public void onFlowStart(Flow flow);

  /**
   * Called when a flow has been finished by the core, that is, when the flow's
   * state has changed to {@link Flow.State#COMPLETED}.
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
