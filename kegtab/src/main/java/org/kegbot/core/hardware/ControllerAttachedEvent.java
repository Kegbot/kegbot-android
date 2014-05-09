package org.kegbot.core.hardware;

import org.kegbot.app.event.Event;

public class ControllerAttachedEvent implements Event {

  private final Controller mController;

  public ControllerAttachedEvent(Controller controller) {
    mController = controller;
  }

  public Controller getController() {
    return mController;
  }

}
