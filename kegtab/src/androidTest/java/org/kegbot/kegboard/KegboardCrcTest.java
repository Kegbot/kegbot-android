/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
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

import junit.framework.TestCase;

/**
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegboardCrcTest extends TestCase {

  public void testCrc() {
    final byte[] testInput = {0x01, 0x02, 0x03, 0x04};
    final int expectedCrc = 0xc54f;
    final int actualCrc = KegboardCrc.crc16Ccitt(testInput, testInput.length);

    int[] table = KegboardCrc.getTable();
    for (int i = 0; i < table.length; i++) {
      System.out.print(table[i] & 0x0ffff);
      System.out.print(" ");
    }

    System.out.println("Expected="
        + String.format("0x%04x", Integer.valueOf(expectedCrc & 0x0ffff)));
    System.out.println("Actual=" + String.format("0x%04x", Integer.valueOf(actualCrc & 0x0ffff)));

    assertEquals(expectedCrc, actualCrc);
  }
}
