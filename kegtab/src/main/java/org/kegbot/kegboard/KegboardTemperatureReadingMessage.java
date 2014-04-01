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
public class KegboardTemperatureReadingMessage extends KegboardMessage {

  public static final int MESSAGE_TYPE = 0x11;

  public static final int TAG_SENSOR_NAME = 0x01;

  public static final int TAG_SENSOR_VALUE = 0x02;

  public KegboardTemperatureReadingMessage(byte[] wholeMessage) throws KegboardMessageException {
    super(wholeMessage);
  }

  public String getName() {
    return readTagAsString(TAG_SENSOR_NAME);
  }

  @Override
  protected String getStringExtra() {
    return String.format("name=%s value=%.2f", getName(), Double.valueOf(getValue()));
  }

  public double getValue() {
    final Long rawValue = readTagAsLong(TAG_SENSOR_VALUE);
    if (rawValue == null) {
      throw new IllegalStateException("Missing tag.");
    }
    return rawValue.doubleValue() / 1e6;
  }

  @Override
  public short getMessageType() {
    return MESSAGE_TYPE;
  }

}
