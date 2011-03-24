package org.kegbot.api;

/**
 * Base exception for {@link KegbotApi} methods.
 */
public class KegbotApiException extends Exception {

  public KegbotApiException() {
  }

  /**
   * @param detailMessage
   */
  public KegbotApiException(String detailMessage) {
    super(detailMessage);
  }

  /**
   * @param throwable
   */
  public KegbotApiException(Throwable throwable) {
    super(throwable);
  }

  /**
   * @param detailMessage
   * @param throwable
   */
  public KegbotApiException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

}
