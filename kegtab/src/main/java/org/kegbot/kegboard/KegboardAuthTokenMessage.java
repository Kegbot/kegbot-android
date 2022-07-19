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

import org.apache.commons.codec.binary.Hex;

/**
 * @author mike
 */
public class KegboardAuthTokenMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x14;

  public static final int TAG_DEVICE_NAME = 0x01;
  public static final int TAG_TOKEN = 0x02;
  public static final int TAG_STATUS = 0x03;

  private static final String CORE_ONEWIRE = "core.onewire";

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

    // Reverse byte order.
    final byte[] reversedBytes = new byte[tokenBytes.length];
    for (int i = 0; i < reversedBytes.length; i++) {
      reversedBytes[tokenBytes.length - i - 1] = tokenBytes[i];
    }
    mComputedToken = Hex.encodeHexString(reversedBytes, true);

    // Rename "onewire" -> "core.onewire"
    if ("onewire".equals(tagName)) {
      mComputedName = CORE_ONEWIRE;
    } else {
      mComputedName = tagName;
    }
  }

  @Override
  protected String getStringExtra() {
    return String.format("name=%s token=%s status=%s", getName(), getToken(), getStatus());
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

  @Override
  public short getMessageType() {
    return MESSAGE_TYPE;
  }

}
