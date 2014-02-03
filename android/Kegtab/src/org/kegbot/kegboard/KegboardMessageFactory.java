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

import com.google.common.primitives.Shorts;

import java.util.Arrays;

public class KegboardMessageFactory {

  private static final boolean DEBUG = false;

  private static final int KBSP_HEADER_LENGTH = 12;

  private static final byte[] KBSP_PREFIX = { 'K', 'B', 'S', 'P', ' ', 'v',
    '1', ':' };

  private final byte[] mBuffer = new byte[2048];

  private int mPayloadLength = 0;
  private int mAppendPosition = 0;
  private int mPosition = 0;

  private boolean mFramingBroken = false;

  public void addBytes(byte[] newBytes, int length) {
    for (int i = 0; i < length; i++) {
      final byte b = newBytes[i];
      if (mFramingBroken) {
        if (b == '\n') {
          mFramingBroken = false;
        }
        continue;
      }
      mBuffer[mAppendPosition++] = b;
    }
  }

  public KegboardMessage getMessage() {
    // Consume from mBuffer until done
    while (available() > 0) {
      if (mPosition < KBSP_HEADER_LENGTH) {
        if (available() < KBSP_HEADER_LENGTH) {
          return null;
        }
        consumeHeader();
        mPayloadLength = Shorts.fromBytes(mBuffer[11], mBuffer[10]);
        if (mPayloadLength > 240) {
          framingError();
          return null;
        }
        continue;
      } else {
        final int totalLength = KBSP_HEADER_LENGTH + mPayloadLength + 4;

        int remain = totalLength - mPosition;
        if (available() < remain) {
          return null;
        }
        debug("completed!");

        final KegboardMessage message;
        try {
          message = KegboardMessage.fromBytes(Arrays.copyOf(mBuffer, totalLength));
          return message;
        } catch (KegboardMessageException e) {
          debug("Error building message: " + e);
        } finally {
          System.arraycopy(mBuffer, totalLength, mBuffer, 0, mBuffer.length - totalLength);
          mPosition = 0;
          mAppendPosition -= totalLength;
        }
      }
    }

    return null;
  }

  private void debug(String message) {
    if (DEBUG) {
      System.out.println("[mPosition=" + mPosition + "]: " + message);
    }
  }

  private void framingError() {
    mFramingBroken = true;
  }

  private int available() {
    return mAppendPosition - mPosition;
  }

  private byte consumeByte() {
    final byte b = mBuffer[mPosition++];
    return b;
  }

  private void consumeHeader() {
    for (int i = 0; i < KBSP_PREFIX.length; i++) {
      final byte b = consumeByte();
      if (b != KBSP_PREFIX[i]) {
        framingError();
        return;
      }
    }

    mPosition += 8;
  }

}
