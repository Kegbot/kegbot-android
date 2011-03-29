package org.kegbot.kegtap.service;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Set;

import org.kegbot.core.AuthenticationToken;
import org.kegbot.core.FlowMeter;
import org.kegbot.core.ThermoSensor;
import org.kegbot.core.net.KegnetMessage;
import org.kegbot.core.net.KegnetServer;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * This service listens to and manages kegbot hardware: attached kegboards,
 * sensors, and so on.
 */
public class KegbotHardwareService extends IntentService {

  private static String TAG = KegbotHardwareService.class.getSimpleName();

  private final Collection<FlowMeter> mFlowMeters = Sets.newLinkedHashSet();

  private final Collection<ThermoSensor> mThermoSensors = Sets.newLinkedHashSet();

  private Set<Listener> mListeners = Sets.newLinkedHashSet();

  public class LocalBinder extends Binder {
    KegbotHardwareService getService() {
      return KegbotHardwareService.this;
    }
  }

  private final IBinder mBinder = new LocalBinder();

  public KegbotHardwareService() {
    super(TAG);
  }

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.v(TAG, "Bound");
    return mBinder;
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.v(TAG, "Starting kegnet server.");

    final KegnetServer server = new KegnetServer("0.0.0.0", 9805);
    try {
      server.bind();
    } catch (UnknownHostException e) {
      Log.e(TAG, "Error binding.", e);
    } catch (IOException e) {
      Log.e(TAG, "Error binding.", e);
    }
    Socket socket = null;
    while (true) {
      if (socket == null) {
        try {
          socket = server.accept();
        } catch (IOException e) {
          Log.e(TAG, "Error accepting.", e);
          return;
        }
      }

      KegnetMessage message = null;
      try {
        message = KegnetServer.getNextMessage(socket);
        if (message == null) {
          throw new IOException("Remote host closed.");
        }
      } catch (IOException e) {
        Log.w(TAG, "Error getting message.", e);
        try {
          socket.close();
        } catch (IOException e1) {
          // Ignore
        } finally {
          socket = null;
        }
      }
      if (message != null) {
        handleKegnetMessage(message);
      }
    }
  }

  /**
   * @param message
   */
  private void handleKegnetMessage(KegnetMessage message) {
    final String eventName = message.getEventName();
    if ("ThermoEvent".equals(eventName)) {
      onThermoUpdate(message);
    } else if ("MeterUpdate".equals(eventName)) {
      onMeterUpdate(message);
    } else if ("TokenAuthEvent".equals(eventName)) {
      onTokenAuthEvent(message);
    }

  }

  /**
   * @param message
   */
  private void onThermoUpdate(KegnetMessage message) {
    Log.d(TAG, "Got Thermo Event");
    final String sensorName = message.getBody().get("sensor_name").getTextValue();
    final double value = message.getBody().get("sensor_value").getDoubleValue();
    final ThermoSensor sensor = getOrCreateThermoSensor(sensorName);
    sensor.setTemperatureC(value);

    for (Listener listener : mListeners) {
      listener.onThermoSensorUpdate(sensor);
    }
  }

  private void onMeterUpdate(KegnetMessage message) {
    Log.d(TAG, "Got Meter Event");
    final String tapName = message.getBody().get("tap_name").getTextValue();
    final long ticks = message.getBody().get("reading").getLongValue();
    final FlowMeter meter = getOrCreateMeter(tapName);
    meter.setTicks(ticks);

    for (Listener listener : mListeners) {
      listener.onMeterUpdate(meter);
    }
  }

  private void onTokenAuthEvent(KegnetMessage message) {
    Log.d(TAG, "Got Auth Token Event");
    final String status = message.getBody().get("status").getTextValue();
    final String authDevice = message.getBody().get("auth_device_name").getTextValue();
    final String value = message.getBody().get("token_value").getTextValue();
    final String tapName = message.getBody().get("tap_name").getTextValue();

    final AuthenticationToken token = new AuthenticationToken(authDevice, value);
    boolean added = status.equals("added");
    for (Listener listener : mListeners) {
      if (added) {
        listener.onTokenAttached(token, tapName);
      } else {
        listener.onTokenRemoved(token, tapName);
      }
    }

  }

  private FlowMeter getOrCreateMeter(String tapName) {
    synchronized (mFlowMeters) {
      for (FlowMeter meter : mFlowMeters) {
        if (meter.getName().equals(tapName)) {
          return meter;
        }
      }
      FlowMeter meter = new FlowMeter(tapName);
      mFlowMeters.add(meter);
      return meter;
    }
  }

  private ThermoSensor getOrCreateThermoSensor(String sensorName) {
    synchronized (mThermoSensors) {
      for (ThermoSensor sensor : mThermoSensors) {
        if (sensor.getName().equals(sensorName)) {
          return sensor;
        }
      }
      ThermoSensor sensor = new ThermoSensor(sensorName);
      mThermoSensors.add(sensor);
      return sensor;
    }
  }

  public Collection<FlowMeter> getAllFlowMeters() {
    return ImmutableList.copyOf(mFlowMeters);
  }

  public Collection<ThermoSensor> getAllThermoSensors() {
    return ImmutableList.copyOf(mThermoSensors);
  }

  public boolean attachListener(Listener listener) {
    return mListeners.add(listener);
  }

  public boolean removeListener(Listener listener) {
    return mListeners.remove(listener);
  }

  /**
   * Receives notifications for hardware events.
   */
  public interface Listener {

    /**
     * Notifies the listener that a flow meter's state has changed.
     * 
     * @param meter
     *          the meter that was updated
     */
    public void onMeterUpdate(FlowMeter meter);

    /**
     * Notifies the listener that a thermo sensor's state has changed.
     * 
     * @param sensor
     *          the sensor that was updated
     */
    public void onThermoSensorUpdate(ThermoSensor sensor);

    /**
     * An authentication token was momentarily swiped.
     * 
     * @param token
     * @param tapName
     */
    public void onTokenSwiped(AuthenticationToken token, String tapName);

    /**
     * A token was attached.
     * 
     * @param token
     * @param tapName
     */
    public void onTokenAttached(AuthenticationToken token, String tapName);

    /**
     * A token was removed.
     * 
     * @param token
     * @param tapName
     */
    public void onTokenRemoved(AuthenticationToken token, String tapName);
  }
}
