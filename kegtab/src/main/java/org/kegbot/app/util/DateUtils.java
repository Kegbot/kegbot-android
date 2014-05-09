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
package org.kegbot.app.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.TimeZone;

/**
 * Date-related utility functions.
 *
 * <p/>
 * No Android dependencies.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class DateUtils {

  /**
   * Returns a timestamp <em>in the local timezone</em> given an
   * ISO8601-formatted timestamp.
   *
   * @param isoString the time stamp
   * @param timeZone the time zone
   * @return a unix timestamp in milliseconds relative to the given time zone
   * @throws IllegalArgumentException if the timestamp cannot be parsed
   */
  public static long dateFromIso8601String(String isoString, TimeZone timeZone) {
    DateTime dt = new DateTime(new DateTime(isoString), DateTimeZone.forTimeZone(timeZone));
    return dt.getMillis();
  }

  public static long dateFromIso8601String(String isoString) {
    return dateFromIso8601String(isoString, TimeZone.getDefault());
  }

}
