/**
 *
 */
package org.kegbot.kegboard;

import junit.framework.TestCase;

/**
 *
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

    System.out.println("Expected=" + String.format("0x%04x", expectedCrc & 0x0ffff));
    System.out.println("Actual=" + String.format("0x%04x", actualCrc & 0x0ffff));

    assertEquals(expectedCrc, actualCrc);
  }
}
