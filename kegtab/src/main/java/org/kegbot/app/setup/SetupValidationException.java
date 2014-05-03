package org.kegbot.app.setup;

/**
 * Created by mikey on 5/2/14.
 */
public class SetupValidationException extends Exception {

  public SetupValidationException() {
  }

  public SetupValidationException(String detailMessage) {
    super(detailMessage);
  }

  public SetupValidationException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

  public SetupValidationException(Throwable throwable) {
    super(throwable);
  }
}
