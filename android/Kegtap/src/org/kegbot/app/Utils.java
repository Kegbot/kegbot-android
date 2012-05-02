package org.kegbot.app;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.kegbot.app.build.BuildInfo;

import android.os.Build;

public class Utils {

  public static final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

  /**
   * Returns a timestamp, for a the local timezone, given an ISO8601-formatted
   * UTC timestamp.
   *
   * @param isoString
   * @return
   * @throws ParseException
   */
  public static long dateFromIso8601String(String isoString) throws ParseException {
    final long dateMillis = ISO8601_FORMAT.parse(isoString).getTime();
    final TimeZone local = TimeZone.getDefault();
    return dateMillis + local.getOffset(dateMillis);
  }

  public static byte[] readFile(String file) throws IOException {
    return readFile(new File(file));
  }

  public static byte[] readFile(File file) throws IOException {
    RandomAccessFile f = new RandomAccessFile(file, "r");

    try {
      long longlength = f.length();
      int length = (int) longlength;
      if (length != longlength) {
        throw new IOException("File size >= 2 GB");
      }

      byte[] data = new byte[length];
      f.readFully(data);
      return data;
    } finally {
      f.close();
    }
  }

  public static String getUserAgent() {
    return new StringBuilder()
      .append("Kegtap/")
      .append(BuildInfo.BUILD_DATE_HUMAN)
      .append(" (Android ")
      .append(Build.VERSION.RELEASE)
      .append("/")
      .append(Build.VERSION.SDK_INT)
      .append("; ")
      .append(Build.MANUFACTURER)
      .append(" ")
      .append(Build.MODEL)
      .append("; ")
      .append(Build.FINGERPRINT)
      .append(")")
      .toString();
  }

}
