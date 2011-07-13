/**
 *
 */
package org.kegbot.kegboard;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegboardMessageException extends Exception {

  public KegboardMessageException() {
  }

  public KegboardMessageException(String detailMessage) {
    super(detailMessage);
  }

  public KegboardMessageException(Throwable throwable) {
    super(throwable);
  }

  public KegboardMessageException(String detailMessage, Throwable throwable) {
    super(detailMessage, throwable);
  }

}
