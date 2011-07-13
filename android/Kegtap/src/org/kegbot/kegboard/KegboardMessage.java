/**
 *
 */
package org.kegbot.kegboard;

import java.util.Arrays;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.primitives.Shorts;

/**
 *
 * @author mike
 */
public class KegboardMessage {

  private final byte[] mPayload;

  private final int mMessageType;

  private static final int KBSP_HEADER_LENGTH = 12;

  private static final int KBSP_TRAILER_LENGTH = 4;

  private static final int KBSP_MIN_LENGTH = KBSP_HEADER_LENGTH
  + KBSP_TRAILER_LENGTH;

  private static final byte[] KBSP_TRAILER_BYTES = {'\r', '\n'};

  private static final int KBSP_MAX_LENGTH = 256;

  private static final int KBSP_PAYLOAD_MAX_LENGTH = KBSP_MAX_LENGTH - KBSP_MIN_LENGTH;

  protected final Map<Integer, byte[]> mTags = Maps.newLinkedHashMap();

  protected static Map<Integer, String> TAG_NAMES = Maps.newLinkedHashMap();

  protected static Map<Integer, KegboardMessageTagFormatter> TAG_FORMATS = Maps.newLinkedHashMap();

  protected KegboardMessage() {
    mPayload = null;
    mMessageType = 0;
    assert (false);
  }

  protected KegboardMessage(byte[] wholeMessage) throws KegboardMessageException {
    if (wholeMessage.length < KBSP_MIN_LENGTH) {
      throw new KegboardMessageException("Raw message size too small: min=" + KBSP_MIN_LENGTH
          + ", actual=" + wholeMessage.length);
    } else if (wholeMessage.length > KBSP_MAX_LENGTH) {
      throw new KegboardMessageException("Raw message size too large: max=" + KBSP_MAX_LENGTH
          + ", actual=" + wholeMessage.length);
    }

    final int payloadLength = Shorts.fromBytes(wholeMessage[11], wholeMessage[10]) & 0x0ffff;
    final int payloadEnd = KBSP_HEADER_LENGTH + payloadLength;
    if (payloadLength > KBSP_PAYLOAD_MAX_LENGTH) {
      throw new KegboardMessageException("Illegal payload size: max=" + KBSP_PAYLOAD_MAX_LENGTH
          + ", actual=" + payloadLength);
    }

    final int totalMessageSize = KBSP_HEADER_LENGTH + payloadLength + KBSP_TRAILER_LENGTH;
    if (wholeMessage.length != totalMessageSize) {
      throw new KegboardMessageException("Input buffer size does not match computed size: "
          + "payloadLength=" + payloadLength + ", needed=" + totalMessageSize
          + ", actual=" + wholeMessage.length);
    }

    if (payloadLength > 0) {
      mPayload = Arrays.copyOfRange(wholeMessage, KBSP_HEADER_LENGTH, payloadEnd);
    } else {
      mPayload = new byte[0];
    }

    final byte[] crcBytes = Arrays.copyOfRange(wholeMessage, payloadEnd, payloadEnd + 2);

    final byte[] trailerBytes = Arrays.copyOfRange(wholeMessage, payloadEnd + 2, payloadEnd + 4);
    if (!Arrays.equals(trailerBytes, KBSP_TRAILER_BYTES)) {
      throw new KegboardMessageException("Illegal trailer value.");
    }

    final int expectedCrc = KegboardCrc.crc16Ccitt(wholeMessage, wholeMessage.length - 4) & 0x0ffff;
    final int computedCrc = Shorts.fromBytes(crcBytes[1], crcBytes[0]) & 0x0ffff;

    if (expectedCrc != computedCrc) {
      throw new KegboardMessageException("Bad CRC: "
          + "expected=" + String.format("0x%04x ", Integer.valueOf(expectedCrc))
          + "computed=" + String.format("0x%04x ", Integer.valueOf(computedCrc)));
    }

    mMessageType = Shorts.fromBytes(wholeMessage[9], wholeMessage[8]);

    // System.out.println(HexDump.dumpHexString(wholeMessage));

    for (int i = 0; i <= (mPayload.length - 2);) {
      final int tagNum = mPayload[i] & 0x00ff;
      final int length = mPayload[i + 1] & 0x00ff;

      i += 2;

      if ((i + length) <= mPayload.length) {
        mTags.put(Integer.valueOf(tagNum),
            Arrays.copyOfRange(mPayload, i, i + length));
      }

      i += length;
    }
  }

  private static void computeCrc(byte[] messageBytes, int length) {

  }

  @Override
  public String toString() {
    Class<? extends KegboardMessage> clazz = this.getClass();

    StringBuilder builder = new StringBuilder();
    builder.append("<");
    builder.append(clazz.getSimpleName());
    builder.append(": ");

    if (clazz == KegboardMessage.class) {
      builder.append("type=");
      builder.append(String.format("0x%04x ", Integer.valueOf(mMessageType)));
    }
    builder.append(getStringExtra());
    builder.append(">");

    return builder.toString();
  }

  protected String getStringExtra() {
    return "";
  }

  public byte[] readTag(int tagNum) {
    return mTags.get(Integer.valueOf(tagNum));
  }

  public Long readTagAsLong(int tagNum) {
    final byte[] tagData = readTag(tagNum);
    if (tagData != null && tagData.length == 4) {
      long result = (tagData[3] & 0xff) << 24;
      result |= (tagData[2] & 0xff) << 16;
      result |= (tagData[1] & 0xff) << 8;
      result |= tagData[0] & 0xff;
      return Long.valueOf(result);
    }
    return null;
  }

  public String readTagAsString(int tagNum) {
    final byte[] tagData = readTag(tagNum);
    if (tagData == null) {
      return null;
    }
    return KegboardMessageTagFormatter.STRING.format(tagData);
  }

  private String formatTag(int tagNum) {
    final byte[] tagData = readTag(tagNum);
    if (tagData == null) {
      return "null";
    }
    KegboardMessageTagFormatter formatter = TAG_FORMATS.get(Integer
        .valueOf(tagNum));
    if (formatter == null) {
      formatter = KegboardMessageTagFormatter.DEFAULT;
    }
    return formatter.format(tagData);
  }

  private static int extractType(final byte[] bytes) {
    return Shorts.fromBytes(bytes[9], bytes[8]);
  }

  public static KegboardMessage fromBytes(final byte[] bytes) throws KegboardMessageException {
    if (bytes.length < KBSP_MIN_LENGTH) {
      throw new IllegalArgumentException("Too small: " + bytes.length);
    } else if (bytes.length > KBSP_MAX_LENGTH) {
      throw new IllegalArgumentException("Too large: " + bytes.length);
    }

    final int messageType = extractType(bytes);

    switch (messageType) {
    case KegboardHelloMessage.MESSAGE_TYPE:
      return new KegboardHelloMessage(bytes);
    case KegboardMeterStatusMessage.MESSAGE_TYPE:
      return new KegboardMeterStatusMessage(bytes);
    case KegboardTemperatureReadingMessage.MESSAGE_TYPE:
      return new KegboardTemperatureReadingMessage(bytes);
    case KegboardOutputStatusMessage.MESSAGE_TYPE:
      return new KegboardOutputStatusMessage(bytes);
    case KegboardOnewirePresenceMessage.MESSAGE_TYPE:
      return new KegboardOnewirePresenceMessage(bytes);
    case KegboardAuthTokenMessage.MESSAGE_TYPE:
      return new KegboardAuthTokenMessage(bytes);
    default:
      return new KegboardMessage(bytes);
    }
  }

}
