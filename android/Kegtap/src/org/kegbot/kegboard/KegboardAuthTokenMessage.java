/**
 *
 */
package org.kegbot.kegboard;

import com.hoho.android.usbserial.util.HexDump;

/**
 *
 * @author mike
 */
public class KegboardAuthTokenMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x14;

  public static final int TAG_DEVICE_NAME = 0x01;
  public static final int TAG_TOKEN = 0x02;
  public static final int TAG_STATUS = 0x03;

  private final String mComputedName;
  private final String mComputedToken;

  public enum Status {
    REMOVED,
    PRESENT,
    UNKNOWN;
  }

  public KegboardAuthTokenMessage(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);

    final String tagName = readTagAsString(TAG_DEVICE_NAME);
    byte[] tokenBytes = readTag(TAG_TOKEN);
    if (tokenBytes == null) {
      tokenBytes = new byte[0];
    }

    if ("onewire".equals(tagName)) {
      final byte[] copyBytes = new byte[tokenBytes.length];
      for (int i = 0; i < copyBytes.length; i++) {
        copyBytes[tokenBytes.length - i - 1] = tokenBytes[i];
      }
      mComputedName = "core.onewire";
      mComputedToken = HexDump.toHexString(copyBytes);
    } else {
      mComputedName = tagName;
      mComputedToken = HexDump.toHexString(tokenBytes);
    }
  }

  @Override
  protected String getStringExtra() {
    return String.format("name=%s token=%s", getName(), getToken());
  }

  public String getName() {
    return mComputedName;
  }

  public String getToken() {
    return mComputedToken;
  }

  public Status getStatus() {
    byte[] statusBytes = readTag(TAG_STATUS);
    if (statusBytes == null || statusBytes.length == 0) {
      return Status.UNKNOWN;
    }
    return statusBytes[0] == 1 ? Status.PRESENT : Status.REMOVED;
  }

}
