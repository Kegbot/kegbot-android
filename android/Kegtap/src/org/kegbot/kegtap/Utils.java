package org.kegbot.kegtap;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

  public static final DateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

  public static Date dateFromIso8601String(String s) throws ParseException {
    return ISO8601_FORMAT.parse(s);
  }

}
