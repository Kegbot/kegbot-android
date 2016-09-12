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

package org.kegbot.core.hardware;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.ThermoSensor;

import java.util.Collection;
import java.util.Map;

public class NetworkControllerManager implements ControllerManager {

  private static NetworkControllerManager sSingleton = null;

  private final Bus mBus;
  private final Listener mListener;

  public static class FakeController implements Controller {

    private final Map<String, FlowMeter> mFlowMeters = Maps.newLinkedHashMap();
    private final Map<String, ThermoSensor> mThermoSensors = Maps.newLinkedHashMap();
    private final String mName;
    private String mStatus;
    private String mSerialNumber;

    public FakeController(final String name, final String status, final String serialNumber) {
      mName = name;
      mStatus = status;
      mSerialNumber = serialNumber;
    }

    @Override
    public String getSerialNumber() {
      return Strings.nullToEmpty(mSerialNumber);
    }

    @Override
    public String getDeviceType() {
      if (getSerialNumber().startsWith("KB-01")) {
        return TYPE_KBPM;
      }
      return TYPE_UNKNOWN;
    }


    @Override
    public String getStatus() {
      return mStatus;
    }

    @Override
    public String getName() {
      return mName;
    }

    @Override
    public Collection<FlowMeter> getFlowMeters() {
      return mFlowMeters.values();
    }

    @Override
    public FlowMeter getFlowMeter(String meterName) {
      return mFlowMeters.get(meterName);
    }

    @Override
    public Collection<ThermoSensor> getThermoSensors() {
      return mThermoSensors.values();
    }

    @Override
    public ThermoSensor getThermoSensor(String sensorName) {
      return mThermoSensors.get(sensorName);
    }

  }

  public NetworkControllerManager(Bus bus, Listener listener) {
    mBus = bus;
    mListener = listener;
  }

  @Override
  public void start() {
    mBus.register(this);
  }

  @Override
  public void stop() {
    mBus.unregister(this);
  }

  @Override
  public void refreshSoon() {
  }

  @Override
  public void dump(IndentingPrintWriter writer) {
  }

  @Subscribe
  public void onFakeControllerEvent(final FakeControllerEvent event) {
    if (event.isAdded()) {
      mListener.onControllerAttached(event.getController());
    } else {
      mListener.onControllerRemoved(event.getController());
    }
  }

}
