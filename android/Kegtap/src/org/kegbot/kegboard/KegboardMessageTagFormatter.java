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

import com.hoho.android.usbserial.util.HexDump;

/**
 *
 * @author mike
 */
public enum KegboardMessageTagFormatter {

  STRING {
    @Override
    public String format(byte[] data) {
      return new String(data);
    }
  },
  INTEGER {
    @Override
    public String format(byte[] data) {
      return String.valueOf(data[0] << 24 | data[1] << 16 | data[2] << 8
          | data[3]);
    }
  },
  BOOLEAN {
    @Override
    public String format(byte[] data) {
      return (data[0] == '0') ? "false" : "true";
    }
  },
  DEFAULT {
    @Override
    public String format(byte[] data) {
      return HexDump.dumpHexString(data);
    }
  };

  public abstract String format(byte[] data);

}
