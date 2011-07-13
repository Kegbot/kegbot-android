/**
 *
 */
package org.kegbot.kegboard;

/**
 *
 * @author mike
 */
public class KegboardOnewirePresenceMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x13;

  public KegboardOnewirePresenceMessage(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);
  }

}
