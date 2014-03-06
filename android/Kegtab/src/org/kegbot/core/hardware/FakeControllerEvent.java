package org.kegbot.core.hardware;

import org.kegbot.app.DebugBroadcastReceiver;
import org.kegbot.app.event.Event;

/**
 * Private event used by {@link DebugBroadcastReceiver} to notify
 * {@link FakeControllerManager}.
 */
public class FakeControllerEvent implements Event {

  final Controller mController;
  final boolean mAdded;

  public FakeControllerEvent(final Controller controller, final boolean added) {
    mController = controller;
    mAdded = added;
  }

  public Controller getController() {
    return mController;
  }

  public boolean isAdded() {
    return mAdded;
  }

}
