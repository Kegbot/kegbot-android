package org.kegbot.app.service;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * This service listens to and manages kegbot hardware: attached kegboards,
 * sensors, and so on.
 */
public class KegnetService extends Service {

  private static String TAG = KegnetService.class.getSimpleName();

  private static final long THERMO_REPORT_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(30);

  private static final String ACTION_METER_UPDATE = "org.kegbot.action.METER_UPDATE";

  private static final String EXTRA_TICKS = "ticks";
  private static final String EXTRA_TAP_NAME = "tap";

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

  private final Map<String, Long> mLastThermoReadingUptimeMillis =
    Maps.newLinkedHashMap();

  private static final IntentFilter DEBUG_INTENT_FILTER = new IntentFilter(ACTION_METER_UPDATE);
  private final BroadcastReceiver mDebugReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (ACTION_METER_UPDATE.equals(action)) {
        String tapName = intent.getStringExtra(EXTRA_TAP_NAME);
        if (Strings.isNullOrEmpty(tapName)) {
          tapName = "kegboard.flow0";
        }

        long ticks = intent.getLongExtra(EXTRA_TICKS, -1);
        if (ticks > 0) {
          Log.d(TAG, "Got debug meter update: tap=" + tapName + " ticks=" + ticks);
          handleMeterUpdate(tapName, ticks);
        }
      }
    }
  };

  /**
   *
   */
  private String mBoardName = "kegboard";

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
    KegnetService getService() {
      return KegnetService.this;
    }
  }

  private final IBinder mBinder = new LocalBinder();

  private final KegboardService.Listener mKegboardListener = new KegboardService.Listener() {
    @Override
    public void onTemperatureReadingMessage(KegboardTemperatureReadingMessage message) {
      final String sensorName = mBoardName + "." + message.getName();
      final Long lastReport = mLastThermoReadingUptimeMillis.get(sensorName);
      final long now = SystemClock.uptimeMillis();
      if (lastReport != null) {
        final long delta = now - lastReport.longValue();
        if (delta < THERMO_REPORT_PERIOD_MILLIS) {
          return;
        }
      }
      mLastThermoReadingUptimeMillis.put(sensorName, Long.valueOf(now));
      handleThermoUpdate(sensorName, message.getValue());
    }

    @Override
    public void onOutputStatusMessage(KegboardOutputStatusMessage message) {
      //
    }

    @Override
    public void onOnewirePresenceMessage(KegboardOnewirePresenceMessage message) {
    }

    @Override
    public void onMeterStatusMessage(KegboardMeterStatusMessage message) {
      handleMeterUpdate(mBoardName + "." + message.getMeterName(), message.getMeterReading());
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
    registerReceiver(mDebugReceiver, DEBUG_INTENT_FILTER);
  }

  @Override
  public void onDestroy() {
    unbindFromKegboardService();
    unregisterReceiver(mDebugReceiver);
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
      final Intent serviceIntent = new Intent(this, KegboardService.class);
      stopService(serviceIntent);
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
    meter.setTicks(ticks);

    for (Listener listener : mListeners) {
      listener.onMeterUpdate(meter);
    }
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
