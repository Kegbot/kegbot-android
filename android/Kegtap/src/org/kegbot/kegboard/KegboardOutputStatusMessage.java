/**
 *
 */
package org.kegbot.kegboard;

/**
 *
 * @author mike
 */
public class KegboardOutputStatusMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x12;

  public KegboardOutputStatusMessage(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);
  }

  @Override
  public short getMessageType() {
    return MESSAGE_TYPE;
  }

}
