package org.kegbot.api;

public class KegbotApiNotFoundError extends KegbotApiServerError {

  public KegbotApiNotFoundError() {
  }

  public KegbotApiNotFoundError(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

  public KegbotApiNotFoundError(String detailMessage) {
    super(detailMessage);
  }

  public KegbotApiNotFoundError(Throwable throwable) {
    super(throwable);
  }

}
