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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.HexDump;

import org.kegbot.core.FlowMeter;
import org.kegbot.core.ThermoSensor;
import org.kegbot.kegboard.KegboardHelloMessage;
import org.kegbot.kegboard.KegboardMessage;
import org.kegbot.kegboard.KegboardMessageFactory;
import org.kegbot.kegboard.KegboardMeterStatusMessage;
import org.kegbot.kegboard.KegboardPingCommand;
import org.kegbot.kegboard.KegboardSetOutputCommand;
import org.kegbot.kegboard.KegboardTemperatureReadingMessage;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A directly-attached Kegboard (Arduino or Pro Mini) USB controller.
 */
public class KegboardController implements Controller {

  private static final String TAG = KegboardController.class.getSimpleName();

  private static final Pattern SERIAL_RE =
      Pattern.compile("^KB-([0-9a-fA-F]{4})-([0-9a-fA-F]{4})-([0-9a-fA-F]{4,8})$");

  static final String METER_0 = "flow0";
  static final String METER_1 = "flow1";

  private static final String DEFAULT_BOARD_NAME = "kegboard";

  private static final long OUTPUT_REFRESH_INTERVAL = TimeUnit.SECONDS.toMillis(5);

  private static final Pattern PORT_NAME_PATTERN = Pattern.compile("[a-zA-Z]+(\\d+)");

  private final UsbSerialPort mSerialPort;

  private String mStatus = STATUS_UNKNOWN;
  private String mName = DEFAULT_BOARD_NAME;
  private String mSerialNumber = "";

  private final byte[] mReadBuffer = new byte[128];

  private final KegboardMessageFactory mReader = new KegboardMessageFactory();

  private final Map<String, FlowMeter> mFlowMetersByName = Maps.newLinkedHashMap();

  private final Map<String, ThermoSensor> mThermoSensors = Maps.newLinkedHashMap();

  /**
   * Map of toggle number to next desired "enable" refresh time.
   */
  private final Map<Integer, Long> mEnabledOutputsToRefreshUptimeMillis = Maps.newLinkedHashMap();

  /** Keys that were present in #mLastEnabledOutputs during last refreshOutputs call. */
  private final Set<Integer> mLastEnabledOutputs = Sets.newLinkedHashSet();

  public KegboardController(UsbSerialPort port) {
    mSerialPort = port;
  }

  @Override
  public String toString() {
    return String.format("<Kegboard '%s': port=%s serial_number=%s status=%s>",
        getName(), mSerialPort, mSerialNumber, mStatus);
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

  void setStatus(String status) {
    mStatus = status;
  }

  @Override
  public synchronized String getName() {
    return mName;
  }

  @Override
  public Collection<FlowMeter> getFlowMeters() {
    return ImmutableList.copyOf(mFlowMetersByName.values());
  }

  @Override
  public FlowMeter getFlowMeter(final String meterName) {
    return mFlowMetersByName.get(meterName);
  }

  @Override
  public Collection<ThermoSensor> getThermoSensors() {
    return mThermoSensors.values();
  }

  @Override
  public ThermoSensor getThermoSensor(String sensorName) {
    return mThermoSensors.get(sensorName);
  }

  UsbSerialPort getPort() {
    return mSerialPort;
  }

  boolean scheduleToggleOutput(final int outputId, final boolean enable) {
    Log.d(TAG, "scheduleToggleOutput: outputId=" + outputId + " enable=" + enable);
    if (outputId < 0 || outputId >= 4) {
      Log.w(TAG, "Unknown output number: " + outputId);
      return false;
    }
    final Integer outputInteger = Integer.valueOf(outputId);

    synchronized (mEnabledOutputsToRefreshUptimeMillis) {
      if (!enable) {
        mEnabledOutputsToRefreshUptimeMillis.remove(outputInteger);
      } else {
        final Long nextRefresh = Long.valueOf(SystemClock.uptimeMillis() + OUTPUT_REFRESH_INTERVAL);
        mEnabledOutputsToRefreshUptimeMillis.put(outputInteger, nextRefresh);
      }
    }

    return true;
  }

  private void toggleOutput(final int outputId, final boolean enable) throws IOException {
    if (outputId < 0 || outputId >= 4) {
      throw new IOException("Illegal output id.");
    }
    final KegboardSetOutputCommand enableCmd = new KegboardSetOutputCommand(outputId, enable);
    mSerialPort.write(enableCmd.toBytes(), 500);
  }

  void refreshOutputs() throws IOException {
    final long now = SystemClock.uptimeMillis();
    synchronized (mEnabledOutputsToRefreshUptimeMillis) {
      for (final Map.Entry<Integer, Long> entry : mEnabledOutputsToRefreshUptimeMillis.entrySet()) {
        final int outputId = entry.getKey().intValue();
        final long deadline = entry.getValue().longValue();
        if (deadline >= now) {
          toggleOutput(outputId, true);
          final Long nextRefresh = Long.valueOf(SystemClock.uptimeMillis() + OUTPUT_REFRESH_INTERVAL);
          mEnabledOutputsToRefreshUptimeMillis.put(entry.getKey(), nextRefresh);
        }
      }

      final Set<Integer> missing = Sets.newLinkedHashSet(mLastEnabledOutputs);
      missing.removeAll(mEnabledOutputsToRefreshUptimeMillis.keySet());
      if (!missing.isEmpty()) {
        for (final Integer outputId : missing) {
          Log.d(TAG, "refreshOutputs: missing id " + outputId);
          toggleOutput(outputId.intValue(), false);
        }
      }

      mLastEnabledOutputs.clear();
      mLastEnabledOutputs.addAll(mEnabledOutputsToRefreshUptimeMillis.keySet());
    }
  }

  void ping() throws IOException {
    final KegboardPingCommand cmd = new KegboardPingCommand();
    mSerialPort.write(cmd.toBytes(), 500);
  }

  synchronized void setSerialNumber(final String serialNumber) {
    if (mSerialNumber.equals(serialNumber)) {
      return;
    }
    if (!mSerialNumber.isEmpty()) {
      Log.w(TAG, String.format("Ignoring serial number change from=%s to=%s.",
          mSerialNumber, serialNumber));
      return;
    }

    final Matcher matcher = SERIAL_RE.matcher(serialNumber);
    if (matcher.matches()) {
      final String g1 = matcher.group(1);
      final String g2 = matcher.group(2);
      final String g3 = Strings.padStart(matcher.group(3), 8, '0');

      Log.d(TAG, "setSerialNumber: " + mSerialNumber);
      mSerialNumber = String.format("KB-%s-%s-%s", g1, g2, g3);
      mName = getShortNameFromSerialNumber(mSerialNumber);
    } else {
      Log.w(TAG, "Unknown serial number, ignoring: " + serialNumber);
    }
  }

  private static String getShortNameFromSerialNumber(final String serialNumber) {
    final int dashPos = serialNumber.lastIndexOf('-');
    if (dashPos != serialNumber.length() - 9) {
      return DEFAULT_BOARD_NAME;
    }
    return String.format("%s-%s",
        DEFAULT_BOARD_NAME, serialNumber.substring(dashPos + 1).toLowerCase(Locale.US));
  }

  /**
   * Reads and returns a single message.
   */
  KegboardMessage readMessage() {
    final KegboardMessage message = mReader.getMessage();
    if (message != null) {
      handleMessage(message);
    }
    return message;
  }

  List<KegboardMessage> readMessages() {
    final List<KegboardMessage> result = Lists.newArrayList();
    while (true) {
      final KegboardMessage message = readMessage();
      if (message == null) {
        break;
      }
      result.add(message);
    }
    return result;
  }


  void blockingRead() throws IOException {
    final int amtRead = mSerialPort.read(mReadBuffer, Integer.MAX_VALUE);
    if (amtRead < 0) {
      Log.w(TAG, "Error!");
      throw new IOException("Device closed.");
    }
    Log.d(TAG, "Read bytes: " + HexDump.dumpHexString(mReadBuffer, 0, amtRead));
    mReader.addBytes(mReadBuffer, amtRead);
  }

  private void handleMessage(KegboardMessage message) {
    if (message instanceof KegboardHelloMessage) {
      // Update cached serial number.
      final String serialNumber = ((KegboardHelloMessage) message).getSerialNumber();
      if (!serialNumber.isEmpty()) {
        Log.d(TAG, "Updating serial number");
        setSerialNumber(serialNumber);
      }
    } else if (message instanceof KegboardMeterStatusMessage) {
      final KegboardMeterStatusMessage meterStatus = (KegboardMeterStatusMessage) message;

      final String portName = meterStatus.getMeterName();
      final String meterName = String.format("%s.%s", getName(), portName);

      final FlowMeter meter;
      if (mFlowMetersByName.containsKey(meterName)) {
        meter = mFlowMetersByName.get(meterName);
      } else {
        meter = new FlowMeter(meterName);
        mFlowMetersByName.put(meterName, meter);
      }
      meter.setTicks(meterStatus.getMeterReading());
    } else if (message instanceof KegboardTemperatureReadingMessage) {
      final KegboardTemperatureReadingMessage tempMessage =
          (KegboardTemperatureReadingMessage) message;

      final String name = tempMessage.getName();
      if (!mThermoSensors.containsKey(name)) {
        mThermoSensors.put(name, new ThermoSensor(name));
      }

      final ThermoSensor sensor = mThermoSensors.get(name);
      sensor.setTemperatureC(tempMessage.getValue());
    }
  }

}
