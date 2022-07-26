package org.kegbot.core.hardware;

import android.os.SystemClock;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import org.kegbot.core.FlowMeter;
import org.kegbot.core.ThermoSensor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by mikey on 9/11/16.
 */

public class NetworkController implements Controller {

    private static final String TAG = NetworkController.class.getSimpleName();

    private final String mHost;
    private final int mPort;
    private final ControllerManager.Listener mListener;

    private String mStatus = Controller.STATUS_UNKNOWN;
    private String mSerialNumber = "";

    private Socket mSocket;
    private BufferedReader mReader;
    private ExecutorService mExecutor;
    private boolean mConnected;

    private AtomicBoolean mStopped = new AtomicBoolean(true);
    private final Map<String, FlowMeter> mFlowMeters = Maps.newLinkedHashMap();
    private final Map<String, ThermoSensor> mThermoSensors = Maps.newLinkedHashMap();

    public NetworkController(String host, int port, ControllerManager.Listener listener) {
        mHost = host;
        mPort = port;
        mListener = listener;

        mSocket = null;
    }

    void start() {
        Log.d(TAG, "Starting!");
        Preconditions.checkState(mStopped.compareAndSet(true, false));

        mExecutor = Executors.newSingleThreadExecutor();
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                networkWorker();
            }
        });
    }

    void stop() {
        Preconditions.checkState(mStopped.compareAndSet(false, true));
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.w(TAG, "Error closing socket: " + e.getMessage());
            }
            mSocket = null;
        }
    }

    private void networkWorker() {
        Log.d(TAG, "Worker starting.");

        while (!mStopped.get()) {
            if (mSocket == null) {
                try {
                    connect();
                } catch (IOException e) {
                    disconnect();
                    SystemClock.sleep(1000);
                    continue;
                }
            }

            final NetworkMessage message = readNextMessage();
            handleMessage(message);
        }
        Log.d(TAG, "Worker exiting ...");
    }

    private void handleMessage(NetworkMessage message) {
        Log.d(TAG, "Got message: " + message);
        if (message instanceof InfoMessage) {
            mSerialNumber = ((InfoMessage) message).deviceId;
            mStatus = Controller.STATUS_OK;
            mListener.onControllerAttached(this);
        } else if (message instanceof StatusMessage) {
            final Map<String, Integer> meters = ((StatusMessage) message).meters;
            for (Map.Entry<String, Integer> entry : meters.entrySet()) {
                // TODO(mikey): Hacky approach to meter name :(
                final String meterName = getName() + "." + entry.getKey().replace("meter", "flow");

                if (!mFlowMeters.containsKey(meterName)) {
                    mFlowMeters.put(meterName, new FlowMeter(meterName));
                }
                final FlowMeter meter = mFlowMeters.get(meterName);

                final long existingTicks = meter.getTicks();
                final long newTicks = entry.getValue();

                // Publish a MeterUpdate event if anything changed.
                if (newTicks != existingTicks) {
                    meter.setTicks(entry.getValue());
                    mListener.onControllerEvent(this, new MeterUpdateEvent(meter));
                }
            }
        } else if (message instanceof ThermoMessage) {
            final Map<String, Double> temps = ((ThermoMessage) message).temps;
            for (Map.Entry<String, Double> entry : temps.entrySet()) {
                // TODO(mikey): Hacky approach to meter name :(
                final String tempName = getName() + "." + entry.getKey().replace("temp_", "thermo-");

                if (!mThermoSensors.containsKey(tempName)) {
                    mThermoSensors.put(tempName, new ThermoSensor(tempName));
                }
                final ThermoSensor temp = mThermoSensors.get(tempName);

                final double existingTemp = temp.getTemperatureC();
                final double newTemp = entry.getValue();

                // Publish a ThermoUpdate event if anything changed.
                if (newTemp != existingTemp) {
                    temp.setTemperatureC(entry.getValue());
                    mListener.onControllerEvent(this, new ThermoSensorUpdateEvent(temp));
                }
            }
        }
    }

    private void connect() throws IOException {
        if (mConnected) {
            return;
        }
        mConnected = true;

        Log.d(TAG, "Connecting to host: " + mHost + ":" + mPort);
        mSocket = new Socket(mHost, mPort);
        mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
        mStatus = Controller.STATUS_NEED_SERIAL_NUMBER;
    }

    private NetworkMessage readNextMessage() {
        Preconditions.checkState(mReader != null);
        String line;
        try {
            line = mReader.readLine();
            if (line == null) {
                throw new IOException("Controller disconnected.");
            }
        } catch (IOException e) {
            Log.w(TAG, "Error reading from network: " + e.getMessage());
            disconnect();
            return null;
        }
        return NetworkMessage.fromString(line.replace("\n", ""));
    }

    private void disconnect() {
        if (!mConnected) {
            return;
        }
        mConnected = false;

        if (mReader != null) {
            try {
                mReader.close();
            } catch (IOException e) {
            } finally {
                mReader = null;
            }
        }

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
            } finally {
                mSocket = null;
            }
        }

        mStatus = Controller.STATUS_UNKNOWN;
        mListener.onControllerRemoved(this);
    }

    @Override
    public String getStatus() {
        return mStatus;
    }

    @Override
    public String getName() {
        return "kegboard-net-" + getSerialNumber();
    }

    @Override
    public String getSerialNumber() {
        return mSerialNumber;
    }

    @Override
    public String getDeviceType() {
        return Controller.TYPE_NETWORK;
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

    static abstract class NetworkMessage {
        @Nullable
        static NetworkMessage fromString(String message) {
            if (message.startsWith(StatusMessage.PREFIX)) {
                return StatusMessage.fromString(message);
            } else if (message.startsWith(ThermoMessage.PREFIX)) {
                return ThermoMessage.fromString(message);
            } else if (message.startsWith(InfoMessage.PREFIX)) {
                return InfoMessage.fromString(message);
            }
            return null;
        }
    }

    static class StatusMessage extends NetworkMessage {
        static final String PREFIX = "kb-status: ";
        final Map<String, Integer> meters;

        StatusMessage(Map<String, Integer> meters) {
            this.meters = Collections.unmodifiableMap(meters);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder("<StatusMessage ");
            builder.append(Joiner.on(' ').withKeyValueSeparator("=").join(meters));
            builder.append('>');
            return builder.toString();
        }

        static StatusMessage fromString(String message) {
            if (!message.startsWith(StatusMessage.PREFIX)) {
                throw new IllegalArgumentException("Invalid message.");
            }
            message = message.substring(PREFIX.length());
            final Map<String, Integer> meters = Maps.newLinkedHashMap();
            for (final String meterReading : Splitter.on(' ').split(message)) {
                final String parts[] = meterReading.split("=");
                if (parts.length != 2) {
                    continue;
                }
                final String name = parts[0].replace(".ticks", "");
                final Integer reading = Integer.valueOf(parts[1]);
                meters.put(name, reading);
            }
            return new StatusMessage(meters);
        }
    }

    static class ThermoMessage extends NetworkMessage {
        static final String PREFIX = "kb-thermo: ";
        final Map<String, Double> temps;

        ThermoMessage(Map<String, Double> temps) {
            this.temps = Collections.unmodifiableMap(temps);
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder("<StatusMessage ");
            builder.append(Joiner.on(' ').withKeyValueSeparator("=").join(temps));
            builder.append('>');
            return builder.toString();
        }

        static ThermoMessage fromString(String message) {
            if (!message.startsWith(ThermoMessage.PREFIX)) {
                throw new IllegalArgumentException("Invalid message.");
            }
            message = message.substring(PREFIX.length());
            final Map<String, Double> temps = Maps.newLinkedHashMap();
            for (final String tempReading : Splitter.on(' ').split(message)) {
                final String parts[] = tempReading.split("=");
                if (parts.length != 2) {
                    continue;
                }
                final String name = parts[0].replace(".temp", "");
                final Double reading = Double.valueOf(parts[1]);
                temps.put(name, reading);
            }
            return new ThermoMessage(temps);
        }
    }


    static class InfoMessage extends NetworkMessage {
        static final String PREFIX = "info: ";
        final String deviceId;
        final String version;

        InfoMessage(final String deviceId, final String version) {
            this.deviceId = deviceId;
            this.version = version;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder("<InfoMessage ");
            builder.append("deviceId=");
            builder.append(this.deviceId);
            builder.append(' ');
            builder.append("version=");
            builder.append(this.version);
            builder.append('>');
            return builder.toString();
        }

        static InfoMessage fromString(String message) {
            if (!message.startsWith(InfoMessage.PREFIX)) {
                throw new IllegalArgumentException("Invalid message.");
            }
            message = message.substring(PREFIX.length());

            final Map<String, String> args = toKeyedMap(message);
            if (args.get("kegboard-particle") == null) {
                throw new IllegalArgumentException("InfoMessage missing hello token");
            }

            final String deviceId = args.get("device_id");
            final String version = args.get("version");
            return new InfoMessage(deviceId, version);
        }
    }

    static Map<String, String> toKeyedMap(final String input) {
        final Map<String, String> result = Maps.newLinkedHashMap();

        for (final String pair : Splitter.on(' ').split(input)) {
            final String parts[] = pair.split("=");
            if (parts.length == 1) {
                result.put(parts[0], "");
            } else {
                result.put(parts[0], parts[1]);
            }
        }
        return result;
    }
}
