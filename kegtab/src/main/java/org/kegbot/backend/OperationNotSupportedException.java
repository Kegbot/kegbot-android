package org.kegbot.backend;

public class OperationNotSupportedException extends BackendException {

  public OperationNotSupportedException() {
    super();
  }

  public OperationNotSupportedException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

  public OperationNotSupportedException(String detailMessage) {
    super(detailMessage);
  }

  public OperationNotSupportedException(Throwable throwable) {
    super(throwable);
  }

}
