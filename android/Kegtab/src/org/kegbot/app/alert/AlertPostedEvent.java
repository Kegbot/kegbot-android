package org.kegbot.app.alert;

import org.kegbot.app.alert.AlertCore.Alert;
import org.kegbot.app.event.Event;

public class AlertPostedEvent implements Event {

  private final Alert mAlert;

  public AlertPostedEvent(final Alert alert) {
    mAlert = alert;
  }

  public Alert getAlert() {
    return mAlert;
  }

}
