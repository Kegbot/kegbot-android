package org.kegbot.api;

public class KegbotApiServerError extends KegbotApiException {

  public KegbotApiServerError() {
    super();
  }

  public KegbotApiServerError(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

  public KegbotApiServerError(String detailMessage) {
    super(detailMessage);
  }

  public KegbotApiServerError(Throwable throwable) {
    super(throwable);
  }

}
