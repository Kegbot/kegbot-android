package org.kegbot.api;

import org.codehaus.jackson.JsonNode;

/**
 * Base exception for {@link KegbotApi} methods.
 */
public class KegbotApiException extends Exception {

  private final JsonNode mErrors;

  public KegbotApiException() {
    mErrors = null;
  }

  public KegbotApiException(JsonNode errors) {
    mErrors = errors;
  }

  /**
   * @param detailMessage
   */
  public KegbotApiException(String detailMessage) {
    super(detailMessage);
    mErrors = null;
  }

  /**
   * @param throwable
   */
  public KegbotApiException(Throwable throwable) {
    super(throwable);
    mErrors = null;
  }

  /**
   * @param detailMessage
   * @param throwable
   */
  public KegbotApiException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
    mErrors = null;
  }

  public JsonNode getErrors() {
    return mErrors;
  }

}
