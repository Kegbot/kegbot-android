package org.kegbot.app.util;

import java.util.Date;

public class DateUtilInterfaces {
  /**
   * An interface for wrapping Time related static/system methods.
   * Implementation of this interface allows the resulting method 
   * calls to be mocked in tests.
   * @author tim.mertens
   *
   */
  public interface Clock {

    /**
     * Get the current time in milliseconds.
     * @return milliseconds from the January 1, 1970 00:00:00.0 UTC
     */
    public long currentTimeMillis();
      
    /**
     * Returns the current time in milliseconds.
     * Deprecated in favor of currentTimeMillis().
     * @see #currentTimeMillis()
     * @return milliseconds from the January 1, 1970 00:00:00.0 UTC
     */
    @Deprecated
    public long elapsedRealtime();
  }
  
  /**
   * An interface for wrapping Date related static/system methods.
   * Implementation of this interface allows the resulting method 
   * calls to be mocked in tests.
   * @author tim.mertens
   *
   */
  public interface Calendar {
    /**
     * Returns the current Date and Time as a Date object.
     * @return
     */
    public Date getDate();
  }
}
