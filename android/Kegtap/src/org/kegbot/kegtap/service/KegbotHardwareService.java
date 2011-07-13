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
import org.kegbot.kegboard.KegboardAuthTokenMessage;
import org.kegbot.kegboard.KegboardHelloMessage;
import org.kegbot.kegboard.KegboardMeterStatusMessage;
import org.kegbot.kegboard.KegboardOnewirePresenceMessage;
import org.kegbot.kegboard.KegboardOutputStatusMessage;
import org.kegbot.kegboard.KegboardTemperatureReadingMessage;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * This service listens to and manages kegbot hardware: attached kegboards,
 * sensors, and so on.
 */
public class KegbotHardwareService extends Service {

  private static String TAG = KegbotHardwareService.class.getSimpleName();

  /**
   * All monitored flow meters.
   */
  private final Collection<FlowMeter> mFlowMeters = Sets.newLinkedHashSet();

  /**
   * All monitored thermo sensors.
   */
  private final Collection<ThermoSensor> mThermoSensors = Sets.newLinkedHashSet();

  /**
   * All listeners.
   */
  private Set<Listener> mListeners = Sets.newLinkedHashSet();

  private KegboardService mKegboardService;
  private boolean mKegboardServiceBound;

  /**
   * Connection to the API service.
   */
  private ServiceConnection mKegboardServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      mKegboardService = ((KegboardService.LocalBinder) service).getService();
      mKegboardService.addListener(mKegboardListener);
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      mKegboardService = null;
    }
  };

  /**
   * Local binder interface.
   */
  public class LocalBinder extends Binder {
    KegbotHardwareService getService() {
      return KegbotHardwareService.this;
    }
  }

  private final IBinder mBinder = new LocalBinder();

  private final KegboardService.Listener mKegboardListener = new KegboardService.Listener() {
    @Override
    public void onTemperatureReadingMessage(KegboardTemperatureReadingMessage message) {
      handleThermoUpdate(message.getName(), message.getValue());
    }

    @Override
    public void onOutputStatusMessage(KegboardOutputStatusMessage message) {
    }

    @Override
    public void onOnewirePresenceMessage(KegboardOnewirePresenceMessage message) {
    }

    @Override
    public void onMeterStatusMessage(KegboardMeterStatusMessage message) {
      handleMeterUpdate("kegboard." + message.getMeterName(), message.getMeterReading());
    }

    @Override
    public void onHelloMessage(KegboardHelloMessage message) {
    }

    @Override
    public void onDeviceDetached() {
    }

    @Override
    public void onDeviceAttached() {
    }

    @Override
    public void onAuthTokenMessage(KegboardAuthTokenMessage message) {
    }
  };

  private final Runnable mKegnetServerRunnable = new Runnable() {
    @Override
    public void run() {
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
  };

  @Override
  public void onCreate() {
    super.onCreate();
    startService(new Intent(this, KegboardService.class));
    bindToKegboardService();
  }

  @Override
  public void onDestroy() {
    unbindFromKegboardService();
    super.onDestroy();
  }

  private void bindToKegboardService() {
    final Intent serviceIntent = new Intent(this, KegboardService.class);
    bindService(serviceIntent, mKegboardServiceConnection, Context.BIND_AUTO_CREATE);
    mKegboardServiceBound = true;
  }

  private void unbindFromKegboardService() {
    if (mKegboardServiceBound) {
      unbindService(mKegboardServiceConnection);
      mKegboardServiceBound = false;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.v(TAG, "Bound");
    return mBinder;
  }

  /**
   * @param message
   */
  private void handleKegnetMessage(KegnetMessage message) {
    final String eventName = message.getEventName();
    if ("ThermoEvent".equals(eventName)) {
      final String sensorName = message.getBody().get("sensor_name").getTextValue();
      final double value = message.getBody().get("sensor_value").getDoubleValue();

      handleThermoUpdate(sensorName, value);
    } else if ("MeterUpdate".equals(eventName)) {
      final String tapName = message.getBody().get("tap_name").getTextValue();
      final long ticks = message.getBody().get("reading").getLongValue();

      handleMeterUpdate(tapName, ticks);
    } else if ("TokenAuthEvent".equals(eventName)) {
      final String status = message.getBody().get("status").getTextValue();
      final boolean added = status.equals("added");
      final String authDevice = message.getBody().get("auth_device_name").getTextValue();
      final String value = message.getBody().get("token_value").getTextValue();
      final String tapName = message.getBody().get("tap_name").getTextValue();

      handleTokenAuthEvent(tapName, authDevice, value, added);
    }

  }

  /**
   * @param message
   */
  private void handleThermoUpdate(String sensorName, double sensorValue) {
    Log.d(TAG, "Got Thermo Event: " + sensorName + "=" + sensorValue);
    final ThermoSensor sensor = getOrCreateThermoSensor(sensorName);
    sensor.setTemperatureC(sensorValue);

    for (Listener listener : mListeners) {
      listener.onThermoSensorUpdate(sensor);
    }
  }

  private void handleMeterUpdate(String tapName, long ticks) {
    Log.d(TAG, "Got Meter Event: " + tapName + "=" + ticks);
    final FlowMeter meter = getOrCreateMeter(tapName);
    Log.d(TAG, "Got Meter: " + meter);
    meter.setTicks(ticks);

    Log.d(TAG, "Updating listeners...");
    for (Listener listener : mListeners) {
      Log.d(TAG, "++listener");
      listener.onMeterUpdate(meter);
    }
    Log.d(TAG, "Done!");
  }

  private void handleTokenAuthEvent(String tapName, String authDevice, String value, boolean added) {
    Log.d(TAG, "Got Auth Token Event");
    final AuthenticationToken token = new AuthenticationToken(authDevice, value);
    for (final Listener listener : mListeners) {
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
