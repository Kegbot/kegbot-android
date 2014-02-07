package org.kegbot.app.alert;

import org.kegbot.app.alert.AlertCore.Alert;
import org.kegbot.app.event.Event;

public class AlertCancelledEvent implements Event {

  private final Alert mAlert;

  public AlertCancelledEvent(final Alert alert) {
    mAlert = alert;
  }

  public Alert getAlert() {
    return mAlert;
  }

}
