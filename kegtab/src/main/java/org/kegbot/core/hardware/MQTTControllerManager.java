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

package org.kegbot.core.hardware;

import android.util.Log;

import com.google.common.base.Strings;
import com.squareup.otto.Bus;

import org.kegbot.app.config.AppConfiguration;
import org.kegbot.app.util.IndentingPrintWriter;

public class MQTTControllerManager implements ControllerManager {
  private static final String TAG = MQTTControllerManager.class.getSimpleName();

  private final Bus mBus;
  private final Listener mListener;
  private final AppConfiguration mConfig;
  private MQTTController mController;

  public MQTTControllerManager(Bus bus, Listener listener, AppConfiguration config) {
    mBus = bus;
    mListener = listener;
    mConfig = config;

    mController = null;
  }

  @Override
  public void start() {
    mBus.register(this);

    final String mqttServer = mConfig.getMqttServer();
    if (!Strings.isNullOrEmpty(mqttServer)) {
      Log.i(TAG, "MQTT controller is configured.");
      
      // Build the broker URL
      String brokerUrl = "tcp://" + mqttServer + ":" + mConfig.getMqttPort();
      
      // Generate a consistent client ID based on server and port (instead of random UUID)
      String clientId = "kegbot-android-" + Math.abs((mqttServer + ":" + mConfig.getMqttPort()).hashCode());
      
      // Get configuration values
      String topicPrefix = mConfig.getMqttTopicPrefix();
      String username = mConfig.getMqttUsername();
      String password = mConfig.getMqttPassword();
      
      // Create and start the controller
      mController = new MQTTController(brokerUrl, clientId, topicPrefix, username, password, mListener);
      mController.start();
      
      Log.i(TAG, "MQTT controller started with broker: " + brokerUrl + 
                 ", topic prefix: " + topicPrefix + 
                 ", client ID: " + clientId);
    } else {
      Log.i(TAG, "MQTT controller is NOT configured.");
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
    // MQTT connections are persistent, no refresh needed
  }

  @Override
  public void dump(IndentingPrintWriter writer) {
    writer.printPair("mqttController", mController);
    if (mController != null) {
      writer.printPair("mqttBroker", mConfig.getMqttServer() + ":" + mConfig.getMqttPort());
      writer.printPair("mqttTopicPrefix", mConfig.getMqttTopicPrefix());
      writer.printPair("mqttUsername", mConfig.getMqttUsername());
      writer.printPair("mqttStatus", mController.getStatus());
      writer.printPair("mqttSerialNumber", mController.getSerialNumber());
      writer.printPair("mqttFlowMeters", mController.getFlowMeters().size());
      writer.printPair("mqttThermoSensors", mController.getThermoSensors().size());
    }
  }

}
