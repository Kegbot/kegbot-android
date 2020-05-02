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

import com.google.common.base.Strings;

/**
 * @author mike
 */
public class KegboardHelloMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x01;

  private static final int TAG_FIRMWARE_VERSION = 0x01;
  private static final int TAG_PROTOCOL_VERSION = 0x02;
  private static final int TAG_SERIAL_NUMBER = 0x03;
  private static final int TAG_UPTIME_MILLIS = 0x04;
  private static final int TAG_UPTIME_DAYS = 0x05;

  public KegboardHelloMessage(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);
  }

  @Override
  public short getMessageType() {
    return MESSAGE_TYPE;
  }

  @Override
  public String getStringExtra() {
    return new StringBuilder()
        .append("firmware_version=")
        .append(getFirmwareVersion())
        .append(" protocol_version=")
        .append(getProtocolVersion())
        .append(" serial_number=")
        .append(getSerialNumber())
        .append(" uptime_days=")
        .append(getUptimeDays())
        .append(" uptime_millis=")
        .append(getUptimeMillis())
        .toString();
  }


  public int getFirmwareVersion() {
    return readTagAsShort(TAG_FIRMWARE_VERSION);
  }

  public int getProtocolVersion() {
    return readTagAsShort(TAG_PROTOCOL_VERSION);
  }

  public long getUptimeDays() {
    final Long value = readTagAsLong(TAG_UPTIME_DAYS);
    if (value != null) {
      return value.longValue();
    }
    return -1;
  }

  public long getUptimeMillis() {
    final Long value = readTagAsLong(TAG_UPTIME_MILLIS);
    if (value != null) {
      return value.longValue();
    }
    return -1;
  }

  public String getSerialNumber() {
    return Strings.nullToEmpty(readTagAsString(TAG_SERIAL_NUMBER));
  }

}
