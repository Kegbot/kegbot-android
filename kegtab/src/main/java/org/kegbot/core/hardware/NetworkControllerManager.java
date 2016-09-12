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

import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.ThermoSensor;

import java.util.Collection;
import java.util.Map;

public class NetworkControllerManager implements ControllerManager {
  private static final String TAG = NetworkControllerManager.class.getSimpleName();

  private static NetworkControllerManager sSingleton = null;

  private final Bus mBus;
  private final Listener mListener;
  private final AppConfiguration mConfig;
  private NetworkController mController;

  public NetworkControllerManager(Bus bus, Listener listener, AppConfiguration config) {
    mBus = bus;
    mListener = listener;
    mConfig = config;

    mController = null;
  }

  @Override
  public void start() {
    mBus.register(this);

    final String networkHost = mConfig.getNetworkControllerHost();
    if (!Strings.isNullOrEmpty(networkHost)) {
      Log.i(TAG, "Network controller is configured.");
      mController = new NetworkController(networkHost, mConfig.getNetworkControllerPort(), mListener);
      mController.start();
    } else {
      Log.i(TAG, "Network controller is NOT configured.");
      mController = null;
    }
  }

  @Override
  public void stop() {
    if (mController != null) {
      mController.stop();
      mController = null;
    }
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
