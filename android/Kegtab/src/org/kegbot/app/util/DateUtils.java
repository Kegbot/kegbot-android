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

import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.kegbot.app.util.DateUtilInterfaces.Clock;
import org.kegbot.app.util.DateUtilInterfaces.Calendar;


/**
 * Date-related utility functions.
 *
 * <p/>
 * No Android dependencies.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class DateUtils  implements Clock, Calendar {
  private static DateUtils mInstance = new DateUtils();
  
  private DateUtils() {}
  
  ////////////////////
  // Calendar interface implementation
  ////////////////////
  
  @Override
  public Date getDate() {
    return new Date();
  }
  
  ////////////////////
  // Clock interface implementation
  ////////////////////
  
  /**
   * Get the current time in milliseconds.
   * @see System#currentTimeMillis
   * @return milliseconds from the January 1, 1970 00:00:00.0 UTC
   */
  @Override
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }
  
  /**
   * Returns the current time in milliseconds.
   * Deprecated in favor of currentTimeMillis().
   * @return
   */
  @Deprecated
  @Override
  public long elapsedRealtime() {
    return currentTimeMillis();
  }

  ///////////////////////
  // Static Public Methods
  ///////////////////////
  
  /**
   * An accessor for the Clock singleton instance.
   * @return
   */
  public static Clock getInstance() {
    return mInstance;
  }
  
  /**
   * A shorter accessor method for the Clock instance.
   * @return
   */
  public static Clock i() {
    return mInstance;
  }
  
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
