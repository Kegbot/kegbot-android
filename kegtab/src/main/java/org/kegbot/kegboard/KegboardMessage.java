/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For
 * more information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kegbot.kegboard;

import com.google.common.collect.Maps;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Shorts;

import java.util.Arrays;
import java.util.Map;

/**
 * Base message type for messages to/from a Kegboard device.
 *
 * @author mike wakerly (opensource@hoho.com)
 * @see <a href="http://kegbot.org/docs/kegboard-guide/">Kegboard Guide</a>
 */
public abstract class KegboardMessage {

  private static final int KBSP_HEADER_LENGTH = 12;

  private static final int KBSP_TRAILER_LENGTH = 4;

  private static final int KBSP_MIN_LENGTH = KBSP_HEADER_LENGTH
  + KBSP_TRAILER_LENGTH;

  private static final byte[] KBSP_HEADER_BYTES = "KBSP v1:".getBytes();

  private static final byte[] KBSP_TRAILER_BYTES = {'\r', '\n'};

  private static final int KBSP_MAX_LENGTH = 256;

  private static final int KBSP_PAYLOAD_MAX_LENGTH = KBSP_MAX_LENGTH - KBSP_MIN_LENGTH;

  protected final Map<Integer, byte[]> mTags = Maps.newLinkedHashMap();

  protected KegboardMessage() {
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

    final byte[] payload;
    if (payloadLength > 0) {
      payload = Arrays.copyOfRange(wholeMessage, KBSP_HEADER_LENGTH, payloadEnd);
    } else {
      payload = new byte[0];
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

    short messageType = Shorts.fromBytes(wholeMessage[9], wholeMessage[8]);
    if (messageType != getMessageType()) {
      throw new KegboardMessageException("Message type mismatch: expected=" + getMessageType()
          + " got=" + messageType);
    }

    // System.out.println(HexDump.dumpHexString(wholeMessage));

    for (int i = 0; i <= (payload.length - 2);) {
      final int tagNum = payload[i] & 0x00ff;
      final int length = payload[i + 1] & 0x00ff;

      i += 2;

      if ((i + length) <= payload.length) {
        mTags.put(Integer.valueOf(tagNum),
            Arrays.copyOfRange(payload, i, i + length));
      }

      i += length;
    }
  }

  private static int getCrc(byte[] payload) {
    byte[] message = Bytes.concat(KBSP_HEADER_BYTES, payload);
    return KegboardCrc.crc16Ccitt(message, message.length);
  }

  public byte[] toBytes() {
    byte[] payload = new byte[0];

    for (Map.Entry<Integer, byte[]> entry : mTags.entrySet()) {
      final byte[] tag = new byte[] {(byte) (entry.getKey().intValue() & 0x0ff)};
      final byte[] value = entry.getValue();
      final int len = value.length & 0x0ff;
      final byte[] length = new byte[] {(byte) len};

      payload = Bytes.concat(payload, tag, length, value);
    }

    byte[] messageType = new byte[] {(byte) (getMessageType() & 0x0ff), 00};
    byte[] messageLength = new byte[] {(byte) (payload.length & 0x0ff), 00};

    byte[] message = Bytes.concat(KBSP_HEADER_BYTES, messageType, messageLength, payload);

    byte[] crc = Shorts.toByteArray((short) (getCrc(message) & 0x0ffff));
    byte[] result = Bytes.concat(message, crc, KBSP_TRAILER_BYTES);
    return result;
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
      builder.append(String.format("0x%04x ", Integer.valueOf(getMessageType())));
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

  public int readTagAsShort(int tagNum) {
    final byte[] tagData = readTag(tagNum);
    if (tagData != null && tagData.length == 2) {
      int result = (tagData[1] & 0xff) << 8;
      result |= tagData[0] & 0xff;
      return result;
    }
    return 0;
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
    return new String(tagData).replace("\0", "");
  }

  private static int extractType(final byte[] bytes) {
    return Shorts.fromBytes(bytes[9], bytes[8]);
  }

  public static KegboardMessage fromBytes(final byte[] bytes) throws KegboardMessageException {
    if (bytes.length < KBSP_MIN_LENGTH) {
      throw new KegboardMessageException("Too small: " + bytes.length);
    } else if (bytes.length > KBSP_MAX_LENGTH) {
      throw new KegboardMessageException("Too large: " + bytes.length);
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
    case KegboardAuthTokenMessage.MESSAGE_TYPE:
      return new KegboardAuthTokenMessage(bytes);
    default:
      throw new KegboardMessageException("Unknown message type");
    }
  }

  public abstract short getMessageType();

}
