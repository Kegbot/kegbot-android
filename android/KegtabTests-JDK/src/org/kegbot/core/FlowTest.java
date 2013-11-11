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

import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import junit.framework.TestCase;

import org.junit.runner.RunWith;
import org.kegbot.app.util.DateUtils;
import org.kegbot.app.util.TimeSeries;
import org.kegbot.proto.Models.KegTap;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests for {@link Flow}.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({DateUtils.class})
public class FlowTest extends TestCase {
  private static final int FAKE_ML_PER_TICK = 3;
  private static final KegTap FAKE_TAP = KegTap.newBuilder()
      .setId(1)
      .setName("fake-tap")
      .setMlPerTick(FAKE_ML_PER_TICK)
      .setMeterName("fake-meter")
      .setRelayName("fake-relay")
      .build();


  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mockStatic(DateUtils.class);
    when(DateUtils.currentEpochTime()).thenReturn((long) 0);
    assertEquals(0,DateUtils.currentEpochTime(),0);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testTickKeeping() {
    Flow flow = new Flow(1, FAKE_TAP, 100);

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
    Flow flow = new Flow(1, FAKE_TAP, 100);

    assertEquals(0, flow.getDurationMs());
    assertFalse(flow.isIdle());
    when(DateUtils.currentEpochTime()).thenReturn((long) 10);
    assertEquals(10, flow.getDurationMs());
    assertFalse(flow.isIdle());

    when(DateUtils.currentEpochTime()).thenReturn((long) 100);
    assertEquals(100, flow.getDurationMs());
    assertTrue(flow.isIdle());

    assertFalse(flow.isFinished());
    flow.setFinished();
    assertTrue(flow.isFinished());

    assertEquals(0, flow.getVolumeMl(), 0.000001);
    assertEquals(TimeSeries.fromString("0:0 100:0"), flow.getTickTimeSeries());
  }

  public void testShout() {
    Flow flow = new Flow(1, FAKE_TAP, 100);

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
    when(DateUtils.currentEpochTime()).thenReturn((long) 1000);
    Flow flow = new Flow(1, FAKE_TAP, 100);
    assertEquals(TimeSeries.fromString("0:0"), flow.getTickTimeSeries());

    flow.addTicks(1);
    assertEquals(TimeSeries.fromString("0:1"), flow.getTickTimeSeries());

    when(DateUtils.currentEpochTime()).thenReturn((long) 1100);
    flow.addTicks(2);
    assertEquals(TimeSeries.fromString("0:1 100:2"), flow.getTickTimeSeries());

    when(DateUtils.currentEpochTime()).thenReturn((long) 1200);
    flow.addTicks(3);
    assertEquals(TimeSeries.fromString("0:1 100:2 200:3"), flow.getTickTimeSeries());

    assertEquals(flow.getTicks(), 6);
  }

}
