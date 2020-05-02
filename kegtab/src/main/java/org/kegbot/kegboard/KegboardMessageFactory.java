/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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

import com.google.common.base.Preconditions;
import com.google.common.primitives.Shorts;

import java.util.Arrays;

import javax.annotation.concurrent.GuardedBy;

public class KegboardMessageFactory {

  private static final boolean DEBUG = true;

  private static final byte[] KBSP_PREFIX = "KBSP v1:".getBytes();
  private static final byte[] KBSP_TRAILER = "\r\n".getBytes();

  private static final int KBSP_HEADER_LENGTH = 12;
  private static final int KBSP_CRC_LENGTH = 2;
  private static final int KBSP_TRAILER_LENGTH = KBSP_CRC_LENGTH + KBSP_TRAILER.length;
  private static final int KBSP_MIN_PACKET_SIZE = KBSP_HEADER_LENGTH + KBSP_TRAILER_LENGTH;

  @GuardedBy("this")
  private final byte[] mBuffer = new byte[2048];

  private int mAppendPosition = 0;

  public synchronized void addBytes(byte[] newBytes, int length) {
    for (int i = 0; i < length; i++) {
      mBuffer[mAppendPosition++] = newBytes[i];
    }
  }

  public synchronized KegboardMessage getMessage() {
    // Consume from mBuffer until done
    while (mAppendPosition > 0) {
      final int available = mAppendPosition;
      if (available < KBSP_MIN_PACKET_SIZE) {
        return null;
      }

      boolean framingError = false;
      for (int i = 0; i < KBSP_PREFIX.length; i++) {
        if (mBuffer[i] != KBSP_PREFIX[i]) {
          compact(i + 1);
          framingError = true;
          break;
        }
      }

      if (framingError) {
        //debug("Framing error: " + HexDump.dumpHexString(mBuffer, 0, 12));
        continue;
      }

      int payloadLength = Shorts.fromBytes(mBuffer[11], mBuffer[10]);
      if (payloadLength > 240) {
        compact(KBSP_HEADER_LENGTH);
        framingError("Illegal payload length");
        return null;
      }

      int totalLength = KBSP_HEADER_LENGTH + payloadLength + KBSP_TRAILER_LENGTH;
      if (available < totalLength) {
        return null;
      }

      final KegboardMessage message;
      try {
        message = KegboardMessage.fromBytes(Arrays.copyOf(mBuffer, totalLength));
        return message;
      } catch (KegboardMessageException e) {
        debug("Error building message: " + e);
        /* Don't return, keep trying. */
      } finally {
        compact(totalLength);
      }
    }
    return null;
  }

  private void compact(int length) {
    mAppendPosition -= length;
    Preconditions.checkState(mAppendPosition >= 0);
    System.arraycopy(mBuffer, length, mBuffer, 0, mBuffer.length - length);
  }

  private void debug(String message) {
    if (DEBUG) {
      System.out.println("[mAppendPosition=" + mAppendPosition + "]: " + message);
    }
  }

  private void framingError(String reason) {
    debug("Framing error: " + reason);
  }

}
