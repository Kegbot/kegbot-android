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


/**
 * @author mike
 */
public class KegboardMeterStatusMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x10;

  public static final int TAG_METER_NAME = 0x01;
  public static final int TAG_METER_READING = 0x02;

  public KegboardMeterStatusMessage(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);
  }

  @Override
  public String getStringExtra() {
    return "meter=" + getMeterName() + " ticks=" + getMeterReading();
  }

  public String getMeterName() {
    return readTagAsString(TAG_METER_NAME);
  }

  public long getMeterReading() {
    Long result = readTagAsLong(TAG_METER_READING);
    if (result != null) {
      return result.longValue();
    }
    return 0;
  }

  @Override
  public short getMessageType() {
    return MESSAGE_TYPE;
  }

}
