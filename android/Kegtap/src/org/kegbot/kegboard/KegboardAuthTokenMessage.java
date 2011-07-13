/**
 *
 */
package org.kegbot.kegboard;

/**
 *
 * @author mike
 */
public class KegboardAuthTokenMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x14;

  public KegboardAuthTokenMessage(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);
  }

}
