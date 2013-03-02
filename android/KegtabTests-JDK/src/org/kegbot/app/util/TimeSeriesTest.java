/*
 * Copyright 2013 Mike Wakerly <opensource@hoho.com>.
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
package org.kegbot.app.util;

import junit.framework.TestCase;

/**
 * Tests for {@link TimeSeries}.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class TimeSeriesTest extends TestCase {

  public void testTimeSeries_noMinResolution_noRebasing() {
    TimeSeries.Builder builder = TimeSeries.newBuilder(0, false);

    builder.add(10, 1);
    builder.add(11, 1);
    builder.add(20, 1);

    assertEquals(TimeSeries.fromString("10:1 11:1 20:1"), builder.build());
  }

  public void testTimeSeries_withMinResolution_noRebasing() {
    TimeSeries.Builder builder = TimeSeries.newBuilder(10, false);

    builder.add(10, 1);
    builder.add(11, 1);
    builder.add(20, 1);

    assertEquals(TimeSeries.fromString("10:2 20:1"), builder.build());
  }

  public void testTimeSeries_withMinResolution_withRebasing() {
    TimeSeries.Builder builder = TimeSeries.newBuilder(10, true);

    builder.add(10, 1);
    builder.add(11, 1);
    builder.add(20, 1);

    assertEquals(TimeSeries.fromString("0:2 10:1"), builder.build());
  }

  public void testToFromString() {
    TimeSeries ts = TimeSeries.fromString("0:2 10:1");
    assertEquals("0:2 10:1", ts.toString());
  }

}
