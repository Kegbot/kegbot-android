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
package org.kegbot.core;

import junit.framework.TestCase;

import org.kegbot.app.util.TimeSeries;
import org.kegbot.core.FlowManager.Clock;

/**
 * Tests for {@link Flow}.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class FlowTest extends TestCase {

  private long mElapsedRealtime = 0;

  private final Clock mFakeClock = new Clock() {
    @Override
    public long elapsedRealtime() {
      return mElapsedRealtime;
    }
  };

  private static final int FAKE_ML_PER_TICK = 3;
  private static final Tap FAKE_TAP = new Tap("fake-tap", FAKE_ML_PER_TICK, "fake-meter",
      "fake-relay");

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mElapsedRealtime = 0;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testTickKeeping() {
    Flow flow = new Flow(mFakeClock, 1, FAKE_TAP, 100);

    assertEquals(0, flow.getTicks());
    assertEquals(0, flow.getVolumeMl(), 0.000001);

    flow.addTicks(10);
    assertEquals(10, flow.getTicks());
    assertEquals(10 * 3, flow.getVolumeMl(), 0.000001);

    assertFalse(flow.isFinished());
    flow.setFinished();
    assertTrue(flow.isFinished());

    try {
      flow.addTicks(10);
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }

    try {
      flow.setFinished();
      fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException e) {
      // Expected.
    }

  }

  public void testTimeKeeping() {
    Flow flow = new Flow(mFakeClock, 1, FAKE_TAP, 100);

    assertEquals(0, flow.getDurationMs());
    assertFalse(flow.isIdle());
    mElapsedRealtime = 10;
    assertEquals(10, flow.getDurationMs());
    assertFalse(flow.isIdle());

    mElapsedRealtime = 100;
    assertEquals(100, flow.getDurationMs());
    assertTrue(flow.isIdle());

    assertFalse(flow.isFinished());
    flow.setFinished();
    assertTrue(flow.isFinished());

    assertEquals(0, flow.getVolumeMl(), 0.000001);
    assertEquals(TimeSeries.fromString("0:0 100:0"), flow.getTickTimeSeries());
  }

  public void testShout() {
    Flow flow = new Flow(mFakeClock, 1, FAKE_TAP, 100);

    assertEquals("", flow.getShout());
    flow.setShout("Foo");
    assertEquals("Foo", flow.getShout());
    flow.setShout(null);
    assertEquals("", flow.getShout());

    assertFalse(flow.isFinished());
    flow.setFinished();
    assertTrue(flow.isFinished());

    // Post-finished shouts are OK, for now.
    flow.setShout("foo");
    assertEquals("foo", flow.getShout());
  }

  public void testTimeSeries() {
    mElapsedRealtime = 1000;
    Flow flow = new Flow(mFakeClock, 1, FAKE_TAP, 100);
    assertEquals(TimeSeries.fromString("0:0"), flow.getTickTimeSeries());

    flow.addTicks(1);
    assertEquals(TimeSeries.fromString("0:1"), flow.getTickTimeSeries());

    mElapsedRealtime = 1100;
    flow.addTicks(2);
    assertEquals(TimeSeries.fromString("0:1 100:2"), flow.getTickTimeSeries());

    mElapsedRealtime = 1200;
    flow.addTicks(3);
    assertEquals(TimeSeries.fromString("0:1 100:2 200:3"), flow.getTickTimeSeries());

    assertEquals(flow.getTicks(), 6);
  }

}
