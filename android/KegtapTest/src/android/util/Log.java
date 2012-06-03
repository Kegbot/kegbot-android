/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
 *
 * This file is part of the Kegtab package from the Kegbot project. For more
 * information on Kegtab or Kegbot, see <http://kegbot.org/>.
 *
 * Kegtab is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2.
 *
 * Kegtab is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Kegtab. If not, see <http://www.gnu.org/licenses/>.
 */
package android.util;

/**
 * Stub android logger for JDK tests.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class Log {

  private static final String DEBUG = "D";
  private static final String INFO = "I";
  private static final String WARNING = "W";
  private static final String VERBOSE = "V";
  private static final String ERROR = "E";
  private static final String WTF = "F";

  private static int log(String level, String tag, String msg) {
    System.out.println(level + "/" + tag + ": " + msg);
    return 0;
  }

  public static int d(String tag, String msg) {
    return log(DEBUG, tag, msg);
  }

  public static int d(String tag, String msg, Throwable tr) {
    return log(DEBUG, tag, msg + ": " + tr.toString());
  }

  public static int i(String tag, String msg) {
    return log(INFO, tag, msg);
  }

  public static int i(String tag, String msg, Throwable tr) {
    return log(INFO, tag, msg + ": " + tr.toString());
  }

  public static int w(String tag, String msg) {
    return log(WARNING, tag, msg);
  }

  public static int w(String tag, String msg, Throwable tr) {
    return log(WARNING, tag, msg + ": " + tr.toString());
  }

  public static int e(String tag, String msg) {
    return log(ERROR, tag, msg);
  }

  public static int e(String tag, String msg, Throwable tr) {
    return log(ERROR, tag, msg + ": " + tr.toString());
  }

  public static int v(String tag, String msg) {
    return log(VERBOSE, tag, msg);
  }

  public static int v(String tag, String msg, Throwable tr) {
    return log(VERBOSE, tag, msg + ": " + tr.toString());
  }

  public static int wtf(String tag, String msg) {
    return log(WTF, tag, msg);
  }

  public static int wtf(String tag, String msg, Throwable tr) {
    return log(WTF, tag, msg + ": " + tr.toString());
  }

}
