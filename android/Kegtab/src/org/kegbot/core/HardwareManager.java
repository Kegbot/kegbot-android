/*
 * Copyright 2012 Mike Wakerly <opensource@hoho.com>.
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.annotation.GuardedBy;
import org.kegbot.app.KegtabBroadcast;
import org.kegbot.app.config.AppConfiguration;
import org.kegbot.kegboard.KegboardAuthTokenMessage;
import org.kegbot.kegboard.KegboardAuthTokenMessage.Status;
import org.kegbot.kegboard.KegboardHelloMessage;
import org.kegbot.kegboard.KegboardMeterStatusMessage;
import org.kegbot.kegboard.KegboardOutputStatusMessage;
import org.kegbot.kegboard.KegboardTemperatureReadingMessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Generic hardware manager. Attaches to hardware-specific managers and exports
 * generic events to the rest of the core.
 */
public class HardwareManager extends Manager {

  private static String TAG = HardwareManager.class.getSimpleName();

  private static final long THERMO_REPORT_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(30);

  private static final String ACTION_METER_UPDATE = "org.kegbot.action.METER_UPDATE";
  private static final String ACTION_TOKEN_DEAUTHED = "org.kegbot.action.TOKEN_DEAUTHED";

  private static final String EXTRA_TICKS = "ticks";
  private static final String EXTRA_TAP_NAME = "tap";

  /**
   * All monitored flow meters.
   */
  @GuardedBy("mFlowMeters")
  private final Map<String, FlowMeter> mFlowMeters = Maps.newLinkedHashMap();

  /**
   * All monitored thermo sensors.
   */
  @GuardedBy("mThermoSensors")
  private final Map<String, ThermoSensor> mThermoSensors = Maps.newLinkedHashMap();

  /**
   * All listeners.
   */
  private Set<Listener> mListeners = Sets.newLinkedHashSet();

  private final Context mContext;
  private final AppConfiguration mConfig;
  private final KegboardManager mKegboardManager;

  private final Map<String, Long> mLastThermoReadingElapsedRealtime =
    Maps.newLinkedHashMap();

  private static final IntentFilter DEBUG_INTENT_FILTER = new IntentFilter();
  static {
    DEBUG_INTENT_FILTER.addAction(ACTION_METER_UPDATE);
    DEBUG_INTENT_FILTER.addAction(KegtabBroadcast.ACTION_TOKEN_ADDED);
    DEBUG_INTENT_FILTER.addAction(ACTION_TOKEN_DEAUTHED);
  }

  private final BroadcastReceiver mDebugReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      Log.d(TAG, "Received action: " + action);

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
      } else if (KegtabBroadcast.ACTION_TOKEN_ADDED.equals(action) || ACTION_TOKEN_DEAUTHED.equals(action)) {
        final Bundle extras = intent.getExtras();
        if (extras == null) {
          return;
        }
        final String tapName = extras.getString(EXTRA_TAP_NAME, "");
        final String authDevice = extras.getString(KegtabBroadcast.TOKEN_ADDED_EXTRA_AUTH_DEVICE, "core.rfid");
        final String value = extras.getString(KegtabBroadcast.TOKEN_ADDED_EXTRA_TOKEN_VALUE, "");
        final boolean added = KegtabBroadcast.ACTION_TOKEN_ADDED.equals(action);
        Log.d(TAG, "Sending token auth event: authDevice=" + authDevice + " value=" + value);
        handleTokenAuthEvent(tapName, authDevice, value, added);
      }
    }
  };

  private final KegboardManager.Listener mKegboardListener = new KegboardManager.Listener() {
    @Override
    public void onTemperatureReadingMessage(KegboardTemperatureReadingMessage message) {
      final String sensorName = formatWithBoardName(message.getName());
      final Long lastReport = mLastThermoReadingElapsedRealtime.get(sensorName);
      final long now = SystemClock.elapsedRealtime();
      if (lastReport != null) {
        final long delta = now - lastReport.longValue();
        if (delta < THERMO_REPORT_PERIOD_MILLIS) {
          return;
        }
      }
      mLastThermoReadingElapsedRealtime.put(sensorName, Long.valueOf(now));
      handleThermoUpdate(sensorName, message.getValue());
    }

    @Override
    public void onOutputStatusMessage(KegboardOutputStatusMessage message) {
    }

    @Override
    public void onMeterStatusMessage(KegboardMeterStatusMessage message) {
      handleMeterUpdate(formatWithBoardName(message.getMeterName()), message.getMeterReading());
    }

    @Override
    public void onHelloMessage(KegboardHelloMessage message) {
    }

    @Override
    public void onAuthTokenMessage(KegboardAuthTokenMessage message) {
      String deviceName = message.getName();
      if ("onewire".equals(deviceName)) {
        deviceName = "core.onewire";
      } else if ("rfid".equals(deviceName)) {
        deviceName = "core.rfid";
      }
      handleTokenAuthEvent("", deviceName, message.getToken(), message.getStatus() == Status.PRESENT);
    }
  };

  public HardwareManager(Context context, AppConfiguration config, KegboardManager kegboardManager) {
    mContext = context;
    mConfig = config;
    mKegboardManager = kegboardManager;
  }

  @Override
  public void start() {
    mContext.registerReceiver(mDebugReceiver, DEBUG_INTENT_FILTER);
    mKegboardManager.addListener(mKegboardListener);
  }

  @Override
  public void stop() {
    mKegboardManager.removeListener(mKegboardListener);
    mContext.unregisterReceiver(mDebugReceiver);
  }

  public void setTapRelayEnabled(Tap tap, boolean enabled) {
    Log.d(TAG, "setTapRelayEnabled tap=" + tap + " enabled=" + enabled);

    if (tap == null) {
      return;
    }
    final String relayName = tap.getRelayName();
    if (Strings.isNullOrEmpty(relayName)) {
      return;
    }

    String relayPrefix = formatWithBoardName("relay");

    if (relayName.startsWith(relayPrefix) && relayName.length() > relayPrefix.length()) {
      int outputId = -1;
      try {
        outputId = Integer.valueOf(relayName.substring(relayPrefix.length())).intValue();
      } catch (NumberFormatException e) {
      }

      if (outputId >= 0) {
        mKegboardManager.enableOutput(outputId, enabled);
      } else {
        Log.w(TAG, String.format("Unparseable relay name: %s", relayName));
      }
    }
  }

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
      if (!mFlowMeters.containsKey(tapName)) {
        mFlowMeters.put(tapName, new FlowMeter(tapName));
      }
      return mFlowMeters.get(tapName);
    }
  }

  private ThermoSensor getOrCreateThermoSensor(String sensorName) {
    synchronized (mThermoSensors) {
      if (!mThermoSensors.containsKey(sensorName)) {
        mThermoSensors.put(sensorName, new ThermoSensor(sensorName));
      }
      return mThermoSensors.get(sensorName);
    }
  }

  private String formatWithBoardName(String sensorName) {
    return String.format("%s.%s", mConfig.getKegboardName(), sensorName);
  }

  public boolean addListener(Listener listener) {
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
