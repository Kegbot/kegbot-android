package org.kegbot.backend;

/**
 * Top-level exception class for backend errors.
 */
public class BackendException extends Exception {

  public BackendException() {
  }

  public BackendException(String detailMessage) {
    super(detailMessage);
  }

  public BackendException(Throwable throwable) {
    super(throwable);
  }

  public BackendException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

}
