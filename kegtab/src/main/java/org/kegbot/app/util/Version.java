/*
 * Copyright 2003-2020 The Kegbot Project contributors <info@kegbot.org>
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

import com.google.common.base.Strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mikey on 5/2/14.
 */
public class Version implements Comparable<Version> {

  public final int MAJOR;
  public final int MINOR;
  public final int MICRO;
  public final String EXTRA;

  public static final Version UNKNOWN = new Version(0, 0, 0, "unknown");

  private static final Pattern VERSION_RE = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)([a-z]+\\d+)?");

  public Version(int major, int minor, int micro, String extra) {
    MAJOR = major;
    MINOR = minor;
    MICRO = micro;
    EXTRA = extra;
  }

  public static Version fromString(String versionStr) {
    final Matcher matcher = VERSION_RE.matcher(versionStr);
    if (!matcher.matches()) {
      return UNKNOWN;
    }
    final int major = Integer.valueOf(matcher.group(1));
    final int minor = Integer.valueOf(matcher.group(2));
    final int micro = Integer.valueOf(matcher.group(3));
    final String extra = Strings.nullToEmpty(matcher.group(4));
    return new Version(major, minor, micro, extra);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Version)) return false;

    Version version = (Version) o;

    if (MAJOR != version.MAJOR) return false;
    if (MICRO != version.MICRO) return false;
    if (MINOR != version.MINOR) return false;
    if (!EXTRA.equals(version.EXTRA)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = MAJOR;
    result = 31 * result + MINOR;
    result = 31 * result + MICRO;
    result = 31 * result + EXTRA.hashCode();
    return result;
  }

  @Override
  public int compareTo(Version other) {
    if (other == UNKNOWN) {
      return this == UNKNOWN ? 0 : 1;
    }
    if (this.MAJOR != other.MAJOR) {
      return this.MAJOR - other.MAJOR;
    }
    if (this.MINOR != other.MINOR) {
      return this.MINOR - other.MINOR;
    }
    return this.MICRO - other.MICRO;
  }
}
