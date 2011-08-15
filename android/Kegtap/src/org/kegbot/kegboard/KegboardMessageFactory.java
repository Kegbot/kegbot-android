package org.kegbot.kegboard;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;

public class KegboardMessageFactory {

  private static final boolean DEBUG = false;

  private static final int KBSP_HEADER_LENGTH = 12;

  private static final byte[] KBSP_PREFIX = { 'K', 'B', 'S', 'P', ' ', 'v',
    '1', ':' };

  private final byte[] mBuffer = new byte[256];

  private int mPayloadLength = 0;
  private int mPosition = 0;

  public List<KegboardMessage> addBytes(byte[] newBytes, int length) {
    final List<KegboardMessage> result = Lists.newArrayList();
    int pos = 0;

    debug("addBytes length=" + length);

    // Consume from newBytes until done
    while (pos < length) {
      if (mPosition < KBSP_PREFIX.length) {
        pos += consumeHeader(newBytes, length, pos);
        continue;
      } else if (mPosition < KBSP_HEADER_LENGTH) {
        int needed = KBSP_HEADER_LENGTH - mPosition;
        int toCopy = ((length - pos) > needed) ? needed : (length - pos);
        System.arraycopy(newBytes, pos, mBuffer, mPosition, toCopy);
        pos += toCopy;
        mPosition += toCopy;

        if (mPosition == KBSP_HEADER_LENGTH) {
          mPayloadLength = Shorts.fromBytes(mBuffer[11], mBuffer[10]);
          debug("accquired payload length: " + mPayloadLength);
        }
        if (mPayloadLength > 240) {
          reset();
        }
        continue;
      } else {
        final int totalLength = KBSP_HEADER_LENGTH + mPayloadLength + 4;

        int remain = totalLength - mPosition;
        debug("need " + remain + " bytes");
        int toCopy = ((length - pos) > remain) ? remain : (length - pos);
        System.arraycopy(newBytes, pos, mBuffer, mPosition, toCopy);

        pos += toCopy;
        mPosition += toCopy;
        if (mPosition == totalLength) {
          debug("completed!");

          final KegboardMessage message;
          try {
            message = KegboardMessage.fromBytes(Arrays.copyOf(mBuffer, totalLength));
            result.add(message);
          } catch (KegboardMessageException e) {
            debug("Error building message: " + e);
          }
          reset();
        }
      }
    }

    return result;
  }

  private void debug(String message) {
    if (DEBUG) {
      System.out.println("[mPosition=" + mPosition + "]: " + message);
    }
  }

  private void reset() {
    debug("---- resetting ---");
    mPosition = 0;
    mPayloadLength = 0;
    Arrays.fill(mBuffer, (byte) 0);
  }

  private void appendByte(byte b) {
    mBuffer[mPosition++] = b;
  }

  private int consumeHeader(byte[] newBytes, int length, int pos) {
    final int needed = KBSP_PREFIX.length - mPosition;
    final int remain = length - pos;
    final int compareLength = (remain >= needed) ? needed : remain;

    debug("Consuming header, pos=" + pos + ", needed=" + needed + ", remain="
        + remain);

    int i;
    for (i = 0; i < compareLength; i++) {
      final byte currByte = newBytes[pos + i];
      if (currByte == KBSP_PREFIX[mPosition]) {
        appendByte(currByte);
      } else {
        debug("FAIL: " + (char) (KBSP_PREFIX[mPosition]));
        debug("ERROR: bad header, got=" + currByte);
        reset();
        if (currByte == KBSP_PREFIX[0]) {
          appendByte(currByte);
        }
        return 1;
      }
    }
    return compareLength;
  }

}
