package org.kegbot.core.hardware;

import org.kegbot.app.event.Event;
import org.kegbot.app.util.IndentingPrintWriter;

public interface ControllerManager {

  /** Callback interface. */
  interface Listener {
    /** Issued when a new controller has been attached. */
    public void onControllerAttached(Controller controller);

    /** Issued when a controller has an event. */
    public void onControllerEvent(Controller controller, Event event);

    /** Issued when a controller has been removed. */
    public void onControllerRemoved(Controller controller);
  }

  public void start();

  public void stop();

  public void refreshSoon();

  public void dump(IndentingPrintWriter writer);

}
