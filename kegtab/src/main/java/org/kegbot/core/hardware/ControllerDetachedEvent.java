package org.kegbot.core.hardware;

import org.kegbot.app.event.Event;

class ControllerDetachedEvent implements Event {

  private final Controller mController;

  public ControllerDetachedEvent(Controller controller) {
    mController = controller;
  }

  public Controller getController() {
    return mController;
  }

}
