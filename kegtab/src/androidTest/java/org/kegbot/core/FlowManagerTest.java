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
package org.kegbot.core;

import android.test.InstrumentationTestCase;

import com.squareup.otto.Bus;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.core.FlowManager.Clock;
import org.kegbot.proto.Models.Controller;
import org.kegbot.proto.Models.FlowMeter;
import org.kegbot.proto.Models.KegTap;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link FlowManager}.
 */
public class FlowManagerTest extends InstrumentationTestCase {

  private static final String METER_0 = "test.flow0";
  private static final String METER_1 = "test.flow1";

  private TapManager mTapManager;
  private AppConfiguration mConfig;
  private Bus mBus;
  private KegTap mTap0;
  private KegTap mTap1;
  private FlowManager mFlowManager;
  private long mElapsedRealtime = 0;

  private final Clock mClock = new Clock() {
    @Override
    public long elapsedRealtime() {
      return mElapsedRealtime;
    }
  };

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // http://stackoverflow.com/q/12267572
    System.setProperty("dexmaker.dexcache",
        getInstrumentation().getTargetContext().getCacheDir().getPath());

    mBus = mock(Bus.class);

    mTapManager = new TapManager(mBus, null);
    mTap0 = KegTap.newBuilder()
        .setId(1)
        .setName("Test Tap 0")
        .setMeter(FlowMeter.newBuilder()
            .setId(1)
            .setPortName("flow0")
            .setName(METER_0)
            .setTicksPerMl(1)
            .setController(Controller.newBuilder()
                .setId(1)
                .setName("test")
                .build())
            .build())
        .build();

    mTap1 = KegTap.newBuilder()
        .setId(2)
        .setName("Test Tap 1")
        .setMeter(FlowMeter.newBuilder()
            .setId(1)
            .setPortName("flow1")
            .setName(METER_1)
            .setTicksPerMl(1)
            .setController(Controller.newBuilder()
                .setId(1)
                .setName("test")
                .build())
            .build())
        .build();

    mTapManager.addTap(mTap0);
    mTapManager.addTap(mTap1);

    mConfig = mock(AppConfiguration.class);
    when(Boolean.valueOf(mConfig.getEnableFlowAutoStart())).thenReturn(Boolean.TRUE);

    mFlowManager = new FlowManager(mBus, mTapManager, mConfig, mClock);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testGetAllActiveFlows() {
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());

    mFlowManager.startFlow(METER_0, 10000);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());

    Flow flow = flows.get(0);
    assertEquals(mTap0, flow.getTap());
    assertEquals(1, flow.getFlowId());

    mFlowManager.endFlow(flow);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());
  }

  public void testEndFlow() {
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());

    final Flow flow = mFlowManager.startFlow(METER_0, 10000);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());

    mFlowManager.endFlow(flow);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());
  }

  public void testGetFlowForTap() {
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());

    final Flow flow = mFlowManager.startFlow(METER_0, 10000);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());

    assertEquals(flow, mFlowManager.getFlowForTap(mTap0));
    assertNull(mFlowManager.getFlowForTap(mTap1));
  }

  @SuppressWarnings("deprecation")
  public void testGetFlowForMeterName() {
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());

    final Flow flow = mFlowManager.startFlow(METER_0, 10000);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());

    assertEquals(flow, mFlowManager.getFlowForMeterName(mTap0.getMeter().getName()));
    assertNull(mFlowManager.getFlowForMeterName(mTap1.getMeter().getName()));
  }

  public void testGetFlowForFlowId() {
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());

    final Flow flow = mFlowManager.startFlow(METER_0, 10000);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());
    assertEquals(1, flow.getFlowId());

    assertSame(flow, mFlowManager.getFlowForFlowId(1));
    assertNull(mFlowManager.getFlowForFlowId(2));
  }

  public void testCreateUpdateReplaceFlow() {
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());

    Flow flow = mFlowManager.handleMeterActivity(mTap0.getMeter().getName(), 100);

    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());
    assertSame(flow, flows.get(0));
    assertEquals(0, flow.getTicks());
    assertEquals(1, flow.getFlowId());

    Flow updatedFlow = mFlowManager.handleMeterActivity(mTap0.getMeter().getName(), 200);
    assertSame(flow, updatedFlow);
    assertEquals(100, flow.getTicks());

    // Rolling the flow meter backwards does not increase flow ticks; zeroes
    // meter on new value.
    updatedFlow = mFlowManager.handleMeterActivity(mTap0.getMeter().getName(), 10);
    assertSame(flow, updatedFlow);
    assertEquals(100, flow.getTicks());
    mFlowManager.handleMeterActivity(mTap0.getMeter().getName(), 11);
    assertEquals(101, flow.getTicks());

    mFlowManager.endFlow(flow);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());
  }

  public void testAuthentication() {
    Flow flow = mFlowManager.startFlow(METER_0, 10000);
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());

    assertTrue(flow.isAnonymous());
    assertFalse(flow.isAuthenticated());
    assertEquals("", flow.getUsername());

    // Add user at tap. No new flow, takes over existing flow.
    mFlowManager.activateUserAtTap(mTap0, "testuser");
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());

    assertSame(flow, flows.get(0));
    assertFalse(flow.isAnonymous());
    assertTrue(flow.isAuthenticated());
    assertEquals("testuser", flow.getUsername());

    // Replace user at tap.  Creates new flow.
    mFlowManager.activateUserAtTap(mTap0, "newuser");
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());
    assertNotSame(flow, flows.get(0));
    Flow newFlow = flows.get(0);
    assertFalse(newFlow.isAnonymous());
    assertTrue(newFlow.isAuthenticated());
    assertEquals("newuser", newFlow.getUsername());
  }

}
