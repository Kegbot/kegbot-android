/**
 * 
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
