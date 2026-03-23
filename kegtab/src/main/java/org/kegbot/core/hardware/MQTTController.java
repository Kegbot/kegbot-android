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

import android.os.SystemClock;
import android.util.Log;

import com.google.common.base.Preconditions;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.ThermoSensor;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MQTT-based controller implementation that connects to a MQTT broker
 * and subscribes to topics for kegboard messages.
 */
public class MQTTController implements Controller, MqttCallback {

    private static final String TAG = MQTTController.class.getSimpleName();

    // MQTT topic constants (base topics, prefix will be applied dynamically)
    private static final String TOPIC_METER = "meter";
    private static final String TOPIC_TEMP = "temp";

    private final String mBrokerUrl;
    private final String mClientId;
    private final String mTopicPrefix;
    private final String mUsername;
    private final String mPassword;
    private final ControllerManager.Listener mListener;

    private String mStatus = Controller.STATUS_UNKNOWN;
    private String mSerialNumber = "";

    private MqttClient mMqttClient;
    private ExecutorService mExecutor;
    private volatile boolean mConnected;

    private AtomicBoolean mStopped = new AtomicBoolean(true);
    private final Map<String, FlowMeter> mFlowMeters = new ConcurrentHashMap<>();
    private final Map<String, ThermoSensor> mThermoSensors = new ConcurrentHashMap<>();

    public MQTTController(String brokerUrl, String clientId, String topicPrefix, 
                         String username, String password, ControllerManager.Listener listener) {
        mBrokerUrl = brokerUrl;
        mClientId = clientId;
        mTopicPrefix = topicPrefix != null ? topicPrefix : "";
        mUsername = username;
        mPassword = password;
        mListener = listener;
        mConnected = false;
    }

    void start() {
        Log.d(TAG, "Starting MQTT Controller!");
        Preconditions.checkState(mStopped.compareAndSet(true, false));

        mExecutor = Executors.newSingleThreadExecutor();
        mExecutor.execute(this::mqttWorker);
    }

    void stop() {
        Preconditions.checkState(mStopped.compareAndSet(false, true));
        Log.d(TAG, "MQTT Controller stopping");
        disconnect();
        if (mExecutor != null && !mExecutor.isShutdown()) {
            mExecutor.shutdown();
        }
    }

    private void mqttWorker() {
        Log.d(TAG, "MQTT Worker starting.");

        while (!mStopped.get()) {
            if (!mConnected) {
                try {
                    connect();
                } catch (MqttException e) {
                    Log.w(TAG, "MQTT connection failed: " + e.getMessage());
                    disconnect();
                    SystemClock.sleep(5000); // Wait 5 seconds before retry
                    continue;
                }
            }

            // Keep the worker alive to maintain connection
            // The actual message handling is done via callback
            SystemClock.sleep(1000);
        }
        Log.d(TAG, "MQTT Worker exiting ...");
    }

    private synchronized void connect() throws MqttException {
        if (mConnected) {
            Log.d(TAG, "Already connected to MQTT broker");
            return;
        }

        Log.d(TAG, "Connecting to MQTT broker: " + mBrokerUrl);
        
        try {
            mMqttClient = new MqttClient(mBrokerUrl, mClientId, new MemoryPersistence());
            mMqttClient.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(30);
            options.setKeepAliveInterval(60);
            options.setAutomaticReconnect(false); // Handle reconnection manually
            
            // Set username and password if provided
            if (mUsername != null && !mUsername.isEmpty()) {
                options.setUserName(mUsername);
                if (mPassword != null && !mPassword.isEmpty()) {
                    options.setPassword(mPassword.toCharArray());
                }
            }

            mMqttClient.connect(options);
            Log.d(TAG, "MQTT client connected successfully");
            
            // Subscribe to topics
            subscribeToTopics();
            
            // Set device as connected immediately since we don't have an info topic
            mSerialNumber = mClientId; // Use client ID as serial number
            mStatus = Controller.STATUS_OK;
            mConnected = true;
            Log.d(TAG, "Setting mConnected = true");
            
            // Always notify about attachment - let HardwareManager handle duplicate prevention
            Log.d(TAG, "MQTT Controller connected, notifying attachment with name: " + getName());
            Log.d(TAG, "Controller instance: " + this);
            mListener.onControllerAttached(this);
            Log.d(TAG, "Successfully connected to MQTT broker");
            
        } catch (MqttException e) {
            Log.e(TAG, "Failed to connect to MQTT broker: " + e.getMessage());
            mConnected = false;
            throw e;
        }
    }

    private void subscribeToTopics() throws MqttException {
        if (mMqttClient != null && mMqttClient.isConnected()) {
            // Subscribe to all numbered meter and temp topics using wildcards
            String meterTopic = mTopicPrefix.isEmpty() ? TOPIC_METER + "/+" : mTopicPrefix + "/" + TOPIC_METER + "/+";
            String tempTopic = mTopicPrefix.isEmpty() ? TOPIC_TEMP + "/+" : mTopicPrefix + "/" + TOPIC_TEMP + "/+";
            
            mMqttClient.subscribe(meterTopic, 1);
            mMqttClient.subscribe(tempTopic, 1);
            
            Log.d(TAG, "Subscribed to topics: " + meterTopic + ", " + tempTopic);
        }
    }

    private synchronized void disconnect() {
        Log.d(TAG, "Disconnect called, mConnected = " + mConnected);
        if (!mConnected) {
            return;
        }
        mConnected = false;
        Log.d(TAG, "Setting mConnected = false");

        if (mMqttClient != null) {
            try {
                if (mMqttClient.isConnected()) {
                    mMqttClient.disconnect();
                }
                mMqttClient.close();
            } catch (MqttException e) {
                Log.w(TAG, "Error disconnecting MQTT client: " + e.getMessage());
            } finally {
                mMqttClient = null;
            }
        }

        mStatus = Controller.STATUS_UNKNOWN;
        mListener.onControllerRemoved(this);
    }

    // MqttCallback implementation
    @Override
    public void connectionLost(Throwable cause) {
        Log.w(TAG, "MQTT connection lost: " + (cause != null ? cause.getMessage() : "Unknown"));
        mConnected = false;
        mStatus = Controller.STATUS_UNRESPONSIVE;
        mListener.onControllerRemoved(this);
        // Worker thread will detect mConnected == false and attempt to reconnect.
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        Log.d(TAG, "Received message on topic '" + topic + "': " + payload);
        
        // Parse the message based on topic structure (e.g., kegbot/meter/0, kegbot/temp/1)
        if (topic.contains("/" + TOPIC_METER + "/")) {
            String meterIndex = extractTopicIndex(topic, TOPIC_METER);
            if (meterIndex != null) {
                handleMeterMessage(meterIndex, payload);
            } else {
                Log.w(TAG, "Could not extract meter index from topic: " + topic);
            }
        } else if (topic.contains("/" + TOPIC_TEMP + "/")) {
            String tempIndex = extractTopicIndex(topic, TOPIC_TEMP);
            if (tempIndex != null) {
                handleTempMessage(tempIndex, payload);
            } else {
                Log.w(TAG, "Could not extract temp index from topic: " + topic);
            }
        }
    }

    private String extractTopicIndex(String topic, String topicType) {
        // Extract the index from topics like "kegbot/meter/0" or "device/temp/1"
        String[] parts = topic.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals(topicType)) {
                return parts[i + 1];
            }
        }
        return null;
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Not used for incoming messages
    }

    private void handleMeterMessage(String meterIndex, String payload) {
        Log.d(TAG, "Processing meter " + meterIndex + " message: " + payload);
        // Each topic contains data for a single meter, payload is just the tick value
        try {
            final long ticks = Long.parseLong(payload.trim());
            final String meterName = buildMeterName(meterIndex);
            
            if (!mFlowMeters.containsKey(meterName)) {
                mFlowMeters.put(meterName, new FlowMeter(meterName));
            }
            final FlowMeter meter = mFlowMeters.get(meterName);
            
            updateMeterIfChanged(meter, ticks);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid meter value for meter " + meterIndex + ": " + payload);
        }
    }

    private void handleTempMessage(String tempIndex, String payload) {
        Log.d(TAG, "Processing temp " + tempIndex + " message: " + payload);
        // Each topic contains data for a single temperature sensor, payload is just the temperature value
        try {
            final double temp = Double.parseDouble(payload.trim());
            final String tempName = buildThermoName(tempIndex);
            
            if (!mThermoSensors.containsKey(tempName)) {
                mThermoSensors.put(tempName, new ThermoSensor(tempName));
            }
            final ThermoSensor sensor = mThermoSensors.get(tempName);
            
            updateThermoIfChanged(sensor, temp);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid temperature value for temp " + tempIndex + ": " + payload);
        }
    }

    private String buildMeterName(String meterIndex) {
        // Build name from index: "0" -> "kegboard-mqtt-clientId.flow0"
        return getName() + ".flow" + meterIndex;
    }

    private String buildThermoName(String tempIndex) {
        // Build name from index: "0" -> "kegboard-mqtt-clientId.thermo-0"
        return getName() + ".thermo-" + tempIndex;
    }

    private void updateMeterIfChanged(FlowMeter meter, long newTicks) {
        final long existingTicks = meter.getTicks();
        if (newTicks != existingTicks) {
            meter.setTicks(newTicks);
            mListener.onControllerEvent(this, new MeterUpdateEvent(meter));
        }
    }

    private void updateThermoIfChanged(ThermoSensor sensor, double newTemp) {
        final double existingTemp = sensor.getTemperatureC();
        if (newTemp != existingTemp) {
            sensor.setTemperatureC(newTemp);
            mListener.onControllerEvent(this, new ThermoSensorUpdateEvent(sensor));
        }
    }

    @Override
    public String getStatus() {
        return mStatus;
    }

    @Override
    public String getName() {
        return "kegboard-mqtt-" + getSerialNumber();
    }

    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public String getDeviceType() {
        return "MQTT Kegboard";
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
