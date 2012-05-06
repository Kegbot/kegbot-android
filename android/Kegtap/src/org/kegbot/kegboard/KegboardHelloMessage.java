/**
 *
 */
package org.kegbot.kegboard;

/**
 *
 * @author mike
 */
public class KegboardHelloMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x01;

  public KegboardHelloMessage(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);
  }

  @Override
  public short getMessageType() {
    return MESSAGE_TYPE;
  }

}
