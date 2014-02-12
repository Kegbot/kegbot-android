package org.kegbot.backend;

/**
 * The requested item was not found.
 */
public class NotFoundException extends BackendException {

  public NotFoundException() {
  }

  public NotFoundException(String detailMessage) {
    super(detailMessage);
  }

  public NotFoundException(Throwable throwable) {
    super(throwable);
  }

  public NotFoundException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

}
