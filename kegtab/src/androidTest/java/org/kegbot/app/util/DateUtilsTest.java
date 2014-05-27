/*
 * Copyright 2014 Bevbot LLC <info@bevbot.com>
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

import junit.framework.TestCase;

import java.text.ParseException;
import java.util.TimeZone;

/**
 * Tests for {@link DateUtils}.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class DateUtilsTest extends TestCase {

  public void testDateFromIso8601String() throws ParseException {

    TimeZone timeZone = TimeZone.getTimeZone("UTC");

    long timestamp = DateUtils.dateFromIso8601String("2013-03-13T20:46:01+00:00", timeZone);
    assertEquals(1363207561000L, timestamp);

    timestamp = DateUtils.dateFromIso8601String("2013-03-13T22:46:01+02:00", timeZone);
    assertEquals(1363207561000L, timestamp);

    timestamp = DateUtils.dateFromIso8601String("2013-03-13T20:46:01Z", timeZone);
    assertEquals(1363207561000L, timestamp);

    timestamp = DateUtils.dateFromIso8601String("2013-02-02T19:17:57Z", timeZone);
    assertEquals(1359832677000L, timestamp);
  }

}
