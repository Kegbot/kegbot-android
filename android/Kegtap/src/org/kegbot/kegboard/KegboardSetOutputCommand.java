/**
 *
 */
package org.kegbot.kegboard;


/**
 *
 * @author mike
 */
public class KegboardSetOutputCommand extends KegboardMessage {

  private static final short MESSAGE_TYPE = 0x84;

  public static final int TAG_OUTPUT_ID = 0x01;
  public static final int TAG_OUTPUT_MODE = 0x02;

  public KegboardSetOutputCommand(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);
  }

  public KegboardSetOutputCommand(int outputId, boolean enabled) {
    mTags.put(Integer.valueOf(TAG_OUTPUT_ID), new byte[] {(byte) (outputId & 0xf)});
    byte[] value;
    if (enabled) {
      value = new byte[] {1, 0};
    } else {
      value = new byte[] {0, 0};
    }
    mTags.put(Integer.valueOf(TAG_OUTPUT_MODE), value);
  }

  @Override
  public short getMessageType() {
    return MESSAGE_TYPE;
  }

}
