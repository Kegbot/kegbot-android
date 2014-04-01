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
