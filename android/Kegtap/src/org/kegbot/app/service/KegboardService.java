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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.annotation.GuardedBy;
import org.kegbot.kegboard.KegboardAuthTokenMessage;
import org.kegbot.kegboard.KegboardHelloMessage;
import org.kegbot.kegboard.KegboardMessage;
import org.kegbot.kegboard.KegboardMessageFactory;
import org.kegbot.kegboard.KegboardMeterStatusMessage;
import org.kegbot.kegboard.KegboardOutputStatusMessage;
import org.kegbot.kegboard.KegboardSetOutputCommand;
import org.kegbot.kegboard.KegboardTemperatureReadingMessage;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
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
public class KegboardService extends Service {

  private final String TAG = KegboardService.class.getSimpleName();

  private static final String ACTION_USB_PERMISSION = KegboardService.class.getCanonicalName()
      + ".ACTION_USB_PERMISSION";

  private static final boolean VERBOSE = false;

  private static final long REFRESH_OUTPUT_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(5);

  private static final long OUTPUT_MAX_AGE_MILLIS = TimeUnit.MINUTES.toMillis(5);

  /**
   * The system's USB service.
   */
  private UsbManager mUsbManager;

  private UsbDevice mUsbDevice;

  /**
   * The device currently in use, or {@code null}.
   */
  private UsbSerialDriver mSerialDevice;

  private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

  private final KegboardMessageFactory mFactory = new KegboardMessageFactory();

  private final Map<Integer, Long> mEnabledOutputs = Maps.newLinkedHashMap();
  private final Map<Integer, Long> mOutputRefreshTime = Maps.newLinkedHashMap();

  final byte[] mKegboardReadBuffer = new byte[256];

  @GuardedBy("this")
  private boolean mRunning = true;

  public interface Listener {
    public void onDeviceAttached();

    public void onDeviceDetached();

    public void onAuthTokenMessage(KegboardAuthTokenMessage message);

    public void onHelloMessage(KegboardHelloMessage message);

    public void onMeterStatusMessage(KegboardMeterStatusMessage message);

    public void onOutputStatusMessage(KegboardOutputStatusMessage message);

    public void onTemperatureReadingMessage(KegboardTemperatureReadingMessage message);
  }

  private final Collection<Listener> mListeners = Sets.newLinkedHashSet();

  private Runnable mKegboardReaderRunner = new Runnable() {

    @Override
    public void run() {
      try {
        final int amtRead = mSerialDevice.read(mKegboardReadBuffer, 500);
        if (amtRead < 0) {
          Log.w(TAG, "Device closed.");
          stopSelf();
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
        stopSelf();
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
        }
      }

      synchronized (KegboardService.this) {
        if (mRunning) {
          mExecutorService.execute(mKegboardReaderRunner);
        }
      }
    }
  };

  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      Log.d(TAG, "Received broadcast: " + action);
      if (ACTION_USB_PERMISSION.equals(action)) {
        synchronized (this) {
          final UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            if (device != null) {
              setUpUsbDevice(device);
            }
          } else {
            Log.d(TAG, "permission denied for device " + device);
          }
        }
      } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
        removeUsbDevice();
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
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
    registerReceiver(mUsbReceiver, filter);

    mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    probeForSerialDevice();
  }

  private void probeForSerialDevice() {
    Log.d(TAG, "Probing for a serial device");
    mSerialDevice = UsbSerialProber.acquire(mUsbManager);
    if (mSerialDevice != null) {
      Log.d(TAG, "Found a device: " + mSerialDevice);
      final UsbDevice usbDevice = mSerialDevice.getDevice();
      if (mUsbManager.hasPermission(usbDevice)) {
        Log.d(TAG, "Have permission.");
        setUpUsbDevice(usbDevice);
      } else {
        Log.d(TAG, "Requesting permission.");
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
            ACTION_USB_PERMISSION), 0);
        mUsbManager.requestPermission(usbDevice, permissionIntent);
      }
    }
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

  private void setUpUsbDevice(UsbDevice device) {
    Log.d(TAG, "Opening serial device");
    mUsbDevice = device;

    try {
      mSerialDevice.open();
      mSerialDevice.setBaudRate(115200);
    } catch (IOException e) {
      Log.e(TAG, "Error opening.", e);
    }
    mExecutorService.execute(mKegboardReaderRunner);
  }

  private void removeUsbDevice() {
    // mExecutorService.shutdown();
  }

  @Override
  public void onDestroy() {
    synchronized (this) {
      mRunning = false;
    }
    mExecutorService.shutdownNow();
    unregisterReceiver(mUsbReceiver);
    super.onDestroy();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent != null) {
      final String action = intent.getAction();
      Log.d(TAG, "Handling intent: " + action);
      if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
        probeForSerialDevice();
      }
    }
    return START_STICKY;
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

}
