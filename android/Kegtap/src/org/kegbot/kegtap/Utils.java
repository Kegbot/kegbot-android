package org.kegbot.kegtap;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

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

}
