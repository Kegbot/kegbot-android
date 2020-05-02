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

import com.google.common.collect.Sets;
import com.squareup.otto.Bus;

import org.kegbot.app.config.ConfigurationStore;
import org.kegbot.proto.Models.Controller;
import org.kegbot.proto.Models.FlowMeter;
import org.kegbot.proto.Models.KegTap;

import java.util.Collections;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TapManager}.
 */
public class TapManagerTest extends InstrumentationTestCase {

  private TapManager mTapManager;
  private Bus mMockBus;
  private ConfigurationStore mMockConfigStore;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    // http://stackoverflow.com/q/12267572
    System.setProperty("dexmaker.dexcache",
        getInstrumentation().getTargetContext().getCacheDir().getPath());

    mMockBus = mock(Bus.class);
    mMockConfigStore = mock(ConfigurationStore.class);
    mTapManager = new TapManager(mMockBus, mMockConfigStore);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  @SuppressWarnings("deprecation")
  public void testBasics() {
    assertEquals(0, mTapManager.getTaps().size());
    assertEquals(0, mTapManager.getVisibleTaps().size());

    final KegTap tap = KegTap.newBuilder()
        .setId(1)
        .setName("Test Tap")
        .setMeter(FlowMeter.newBuilder()
            .setId(1)
            .setName("test.flow0")
            .setPortName("flow0")
            .setTicksPerMl(1.0f)
            .setController(Controller.newBuilder()
                .setId(1)
                .setName("test")
                .build())
            .build())
        .build();

    mTapManager.addTap(tap);

    assertEquals(Collections.singletonList(tap), mTapManager.getTaps());
    assertEquals(Collections.singletonList(tap), mTapManager.getVisibleTaps());
    assertNull(mTapManager.getTapForMeterName("unknown"));
    assertSame(tap, mTapManager.getTapForMeterName("test.flow0"));

    final Set<String> hiddenTaps = Sets.newHashSet();
    hiddenTaps.add("1");
    hiddenTaps.add("2"); // bogus tap id, preserved across add/remove
    when(mMockConfigStore.getStringSet(
        TapManager.KEY_HIDDEN_TAP_IDS, Collections.<String>emptySet()))
        .thenReturn(hiddenTaps);

    assertEquals(Collections.singletonList(tap), mTapManager.getTaps());
    assertEquals(0, mTapManager.getVisibleTaps().size());

    mTapManager.setTapVisibility(tap, true);
    verify(mMockConfigStore).putStringSet(
        TapManager.KEY_HIDDEN_TAP_IDS, Collections.singleton("2"));
  }

}
