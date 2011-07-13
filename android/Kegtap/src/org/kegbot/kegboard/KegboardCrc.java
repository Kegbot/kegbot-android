/**
 *
 */
package org.kegbot.kegboard;

import com.google.common.annotations.VisibleForTesting;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegboardCrc {

  private static int[] sTable;

  public static int crc16CcittUpdate(int crc, byte b) {
    int bval = b & 0xff;
    bval ^= crc & 0x00ff;
    bval ^= (bval << 4) & 0x00ff;
    int result = ((bval << 8) & 0xffff) | ((crc >> 8) & 0x00ff);
    result ^= bval >> 4;
    result ^= (bval << 3) & 0xffff;
    result &= 0x0ffff;
    return result;
  }

  @VisibleForTesting
  protected static synchronized int[] getTable() {
    if (sTable == null) {
      sTable = new int[256];
      for (int i = 0; i < sTable.length; i++) {
        sTable[i] = crc16CcittUpdate(0, (byte) i);
      }
    }
    return sTable;
  }

  public static int crc16Ccitt(byte[] bytes, int length) {
    int crc = 0;
    final int[] table = getTable();
    for (int i = 0; i < length; i++) {
      final byte b = bytes[i];
      crc = (crc >> 8) ^ table[(crc ^ b) & 0x00ff];
      crc &= 0x0ffff;
    }
    return crc;
  }

}
