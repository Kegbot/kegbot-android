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
package org.kegbot.app.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.kegbot.kegboard.KegboardAuthTokenMessage;
import org.kegbot.kegboard.KegboardHelloMessage;
import org.kegbot.kegboard.KegboardMessage;
import org.kegbot.kegboard.KegboardMessageFactory;
import org.kegbot.kegboard.KegboardMeterStatusMessage;
import org.kegbot.kegboard.KegboardOutputStatusMessage;
import org.kegbot.kegboard.KegboardPingCommand;
import org.kegbot.kegboard.KegboardSetOutputCommand;
import org.kegbot.kegboard.KegboardTemperatureReadingMessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;

/**
 * Monitors a serial Kegboard device, sending updates to any attached listener.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegboardService extends BackgroundService {

  private final String TAG = KegboardService.class.getSimpleName();

  private static final String ACTION_USB_PERMISSION = KegboardService.class.getCanonicalName()
      + ".ACTION_USB_PERMISSION";

  private static final boolean VERBOSE = false;

  private static final long REFRESH_OUTPUT_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(5);

  private static final long OUTPUT_MAX_AGE_MILLIS = TimeUnit.MINUTES.toMillis(5);

  private static final long IDLE_POLL_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(2);

  /**
   * The system's USB service.
   */
  private UsbManager mUsbManager;

  /**
   * The device currently in use, or {@code null}.
   */
  private UsbSerialDriver mSerialDevice;

  private final KegboardMessageFactory mFactory = new KegboardMessageFactory();

  private final Map<Integer, Long> mEnabledOutputs = Maps.newLinkedHashMap();
  private final Map<Integer, Long> mOutputRefreshTime = Maps.newLinkedHashMap();

  final byte[] mKegboardReadBuffer = new byte[256];

  private enum State {
    IDLE,
    RUNNING,
    STOPPING,
    FINISHED;
  }

  private State mState = State.IDLE;

  public interface Listener {
    public void onAuthTokenMessage(KegboardAuthTokenMessage message);

    public void onHelloMessage(KegboardHelloMessage message);

    public void onMeterStatusMessage(KegboardMeterStatusMessage message);

    public void onOutputStatusMessage(KegboardOutputStatusMessage message);

    public void onTemperatureReadingMessage(KegboardTemperatureReadingMessage message);
  }

  private final Collection<Listener> mListeners = Sets.newLinkedHashSet();

  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      Log.d(TAG, "Received broadcast: " + action);
      if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
        Log.d(TAG, "Device detached");
        onUsbDeviceDetached();
      } else {
        Log.d(TAG, "Unknown action: " + action);
      }
    }
  };

  /**
   * Local binder interface.
   */
  public class LocalBinder extends Binder {
    KegboardService getService() {
      return KegboardService.this;
    }
  }

  private final IBinder mBinder = new LocalBinder();

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate");

    final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED); // never fired by framework :-\
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    registerReceiver(mUsbReceiver, filter);
  }

  public void enableOutput(int outputId, boolean enabled) {
    if (outputId < 0 || outputId > 5) {
      throw new IllegalArgumentException("Bad output number: " + outputId);
    }
    Log.d(TAG, "Setting output=" + outputId + " enabled=" + enabled);
    final Integer key = Integer.valueOf(outputId);

    synchronized (this) {
      if (enabled) {
        final Long now = Long.valueOf(SystemClock.uptimeMillis());
        mEnabledOutputs.put(key, now);
      } else {
        mEnabledOutputs.remove(key);
      }
    }
  }

  @Override
  public void onDestroy() {
    unregisterReceiver(mUsbReceiver);
    stateChange(State.FINISHED);
    super.onDestroy();
  }

  public boolean addListener(final Listener listener) {
    return mListeners.add(listener);
  }

  public boolean removeListener(final Listener listener) {
    return mListeners.remove(listener);
  }

  public void notifyListeners(final KegboardMessage message) {
    for (final Listener listener : mListeners) {
      if (message instanceof KegboardAuthTokenMessage) {
        listener.onAuthTokenMessage((KegboardAuthTokenMessage) message);
      } else if (message instanceof KegboardHelloMessage) {
        listener.onHelloMessage((KegboardHelloMessage) message);
      } else if (message instanceof KegboardMeterStatusMessage) {
        listener.onMeterStatusMessage((KegboardMeterStatusMessage) message);
      } else if (message instanceof KegboardOutputStatusMessage) {
        listener.onOutputStatusMessage((KegboardOutputStatusMessage) message);
      } else if (message instanceof KegboardTemperatureReadingMessage) {
        listener.onTemperatureReadingMessage((KegboardTemperatureReadingMessage) message);
      }
    }
  }

  @Override
  protected void runInBackground() {
    mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    Log.d(TAG, "Usb manager: " + mUsbManager);
    if (mUsbManager == null) {
      Log.e(TAG, "No USB manager, exiting.");
      return;
    }
    mainLoop();
  }

  private void mainLoop() {
    Log.d(TAG, "Main loop running.");

    try {
      while (true) {
        switch (mState) {
          case IDLE:
            if (acquireSerialDevice()) {
              Log.d(TAG, "Serial device found, switching to running!");
              stateChange(State.RUNNING);
              tryPing();
            } else {
              SystemClock.sleep(IDLE_POLL_INTERVAL_MILLIS);
            }
            break;
          case RUNNING:
            serviceSerialDevice();
            break;
          case STOPPING:
            releaseSerialDevice();
            stateChange(State.IDLE);
            break;
          case FINISHED:
            releaseSerialDevice();
            return;
          default:
            Log.wtf(TAG, "Unknown state: " + mState);
            break;
        }
      }
    } finally {
      Log.d(TAG, "Main loop exiting.");
    }
  }

  private void stateChange(State newState) {
    Log.d(TAG, "State change: " + mState + " -> " + newState);
    mState = newState;
  }

  /**
   * Finds and acquires a usable UsbSerialDriver, returning {@code true} if valid.
   */
  private boolean acquireSerialDevice() {
    UsbSerialDriver result = UsbSerialProber.acquire(mUsbManager);
    if (result != null) {
      Log.d(TAG, "FindUsbSerialDevice, result=" + result);
      boolean opened = false;
      try {
        result.open();
        opened = true;
        result.setBaudRate(115200);
        Log.d(TAG, "Usb device opened!");
        mSerialDevice = result;
        return true;
      } catch (IOException e) {
        if (opened) {
          try {
            result.close();
          } catch (IOException e1) {
            // Ignore
          }
        }
      }
    }
    return false;
  }

  /**
   * Releases the previously-acquired serial device. No-op if none currently
   * bound.
   */
  private void releaseSerialDevice() {
    if (mSerialDevice != null) {
      try {
        mSerialDevice.close();
      } catch (IOException e) {
        // Ignore
      }
      mSerialDevice = null;
    }
  }

  private void tryPing() {
    KegboardPingCommand command = new KegboardPingCommand();
    try {
      mSerialDevice.write(command.toBytes(), 500);
    } catch (IOException e) {
      // Ignore.
    }
  }

  /**
   * Performs a single step of servicing the serial device.
   */
  private void serviceSerialDevice() {
    try {
      final int amtRead = mSerialDevice.read(mKegboardReadBuffer, 500);
      if (amtRead < 0) {
        Log.w(TAG, "Device closed.");
        stateChange(State.STOPPING);
        return;
      }

      if (amtRead > 0) {
        if (VERBOSE) {
          Log.d(TAG, "Read " + amtRead + " bytes from kegboard");
          Log.d(TAG, HexDump.dumpHexString(mKegboardReadBuffer, 0, amtRead));
        }

        final List<KegboardMessage> newMessages = mFactory.addBytes(mKegboardReadBuffer, amtRead);
        for (final KegboardMessage message : newMessages) {
          Log.i(TAG, "Received message: " + message);
          notifyListeners(message);
        }

        Arrays.fill(mKegboardReadBuffer, (byte) 0);
      }
    } catch (IOException e) {
      Log.e(TAG, "IOException reading from serial.");
      stateChange(State.STOPPING);
      return;
    }

    final long now = SystemClock.uptimeMillis();

    Set<Integer> outputs = Sets.newLinkedHashSet();
    synchronized (this) {
      outputs.addAll(mEnabledOutputs.keySet());
      outputs.addAll(mOutputRefreshTime.keySet());
    }

    // Post new messages
    for (Integer outputId : outputs) {
      boolean enable = true;
      if (!mEnabledOutputs.containsKey(outputId)) {
        enable = false;
        mOutputRefreshTime.remove(outputId);
      } else if (!mOutputRefreshTime.containsKey(outputId)) {
        enable = true;
        mOutputRefreshTime.put(outputId, Long.valueOf(now));
      } else {
        final long lastTime = mOutputRefreshTime.get(outputId).longValue();
        final long age = (now - lastTime);
        if (age < REFRESH_OUTPUT_PERIOD_MILLIS) {
          continue;
        } else if (age > OUTPUT_MAX_AGE_MILLIS) {
          enable = false;
          mEnabledOutputs.remove(outputId);
          mOutputRefreshTime.remove(outputId);
        } else {
          enable = true;
          mOutputRefreshTime.put(outputId, Long.valueOf(now));
        }
      }
      KegboardSetOutputCommand enableCmd = new KegboardSetOutputCommand(outputId.intValue(), enable);
      Log.d(TAG, "Setting relay=" + outputId + " enabled=" + enable);

      final byte[] bytes = enableCmd.toBytes();
      try {
        mSerialDevice.write(bytes, 500);
      } catch (IOException e) {
        Log.e(TAG, "Error writing enable command: " + e.toString(), e);
        stateChange(State.STOPPING);
        return;
      }
    }
  }

  private void onUsbDeviceDetached() {
    stateChange(State.STOPPING);
  }

}
