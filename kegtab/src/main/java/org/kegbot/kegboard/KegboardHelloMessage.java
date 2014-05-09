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

import com.google.common.base.Strings;

/**
 * @author mike
 */
public class KegboardHelloMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x01;

  private static final int TAG_FIRMWARE_VERSION = 0x01;
  private static final int TAG_PROTOCOL_VERSION = 0x02;
  private static final int TAG_SERIAL_NUMBER = 0x03;

  public KegboardHelloMessage(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);
  }

  @Override
  public short getMessageType() {
    return MESSAGE_TYPE;
  }

  @Override
  public String getStringExtra() {
    return "firmware_version=" + getFirmwareVersion() +
        " protocol_version=" + getProtocolVersion() +
        " serial_number=" + getSerialNumber();
  }


  public int getFirmwareVersion() {
    return readTagAsShort(TAG_FIRMWARE_VERSION);
  }

  public int getProtocolVersion() {
    return readTagAsShort(TAG_PROTOCOL_VERSION);
  }

  public String getSerialNumber() {
    return Strings.nullToEmpty(readTagAsString(TAG_SERIAL_NUMBER));
  }

}
