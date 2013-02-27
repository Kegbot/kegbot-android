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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import junit.framework.TestCase;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.core.FlowManager.Clock;

/**
 * Tests for {@link FlowManager}.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class FlowManagerTest extends TestCase {

  private TapManager mTapManager;
  private AppConfiguration mConfig;
  private Tap mTap0;
  private Tap mTap1;
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
    mTapManager = new TapManager();
    mTap0 = new Tap("tap0", 1, "kegboard.flow0", null);
    mTap1 = new Tap("tap1", 1, "kegboard.flow1", null);
    mTapManager.addTap(mTap0);
    mTapManager.addTap(mTap1);

    mConfig = mock(AppConfiguration.class);
    when(Boolean.valueOf(mConfig.getEnableFlowAutoStart())).thenReturn(Boolean.TRUE);

    mFlowManager = new FlowManager(mTapManager, mConfig, mClock);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testGetAllActiveFlows() {
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());

    mFlowManager.startFlow(mTap0, 10000);
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

    final Flow flow = mFlowManager.startFlow(mTap0, 10000);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());

    mFlowManager.endFlow(flow);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());
  }

  public void testGetFlowForTap() {
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());

    final Flow flow = mFlowManager.startFlow(mTap0, 10000);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());

    assertEquals(flow, mFlowManager.getFlowForTap(mTap0));
    assertNull(mFlowManager.getFlowForTap(mTap1));
  }

  public void testGetFlowForMeterName() {
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());

    final Flow flow = mFlowManager.startFlow(mTap0, 10000);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());

    assertEquals(flow, mFlowManager.getFlowForMeterName(mTap0.getMeterName()));
    assertNull(mFlowManager.getFlowForMeterName(mTap1.getMeterName()));
  }

  public void testGetFlowForFlowId() {
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());

    final Flow flow = mFlowManager.startFlow(mTap0, 10000);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());
    assertEquals(1, flow.getFlowId());

    assertSame(flow, mFlowManager.getFlowForFlowId(1));
    assertNull(mFlowManager.getFlowForFlowId(2));
  }

  public void testCreateUpdateReplaceFlow() {
    List<Flow> flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());

    Flow flow = mFlowManager.handleMeterActivity(mTap0.getMeterName(), 100);

    flows = mFlowManager.getAllActiveFlows();
    assertEquals(1, flows.size());
    assertSame(flow, flows.get(0));
    assertEquals(0, flow.getTicks());
    assertEquals(1, flow.getFlowId());

    Flow updatedFlow = mFlowManager.handleMeterActivity(mTap0.getMeterName(), 200);
    assertSame(flow, updatedFlow);
    assertEquals(100, flow.getTicks());

    // Rolling the flow meter backwards does not increase flow ticks; zeroes
    // meter on new value.
    updatedFlow = mFlowManager.handleMeterActivity(mTap0.getMeterName(), 10);
    assertSame(flow, updatedFlow);
    assertEquals(100, flow.getTicks());
    mFlowManager.handleMeterActivity(mTap0.getMeterName(), 11);
    assertEquals(101, flow.getTicks());

    mFlowManager.endFlow(flow);
    flows = mFlowManager.getAllActiveFlows();
    assertEquals(0, flows.size());
  }

  public void testAuthentication() {
    Flow flow = mFlowManager.startFlow(mTap0, 10000);
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
