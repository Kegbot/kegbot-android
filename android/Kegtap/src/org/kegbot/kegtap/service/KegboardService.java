/**
 * Copyright 2011 mike wakerly (mike@wakerly.com). All rights reserved.
 * Confidential and proprietary.
 */
package org.kegbot.kegtap.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.kegbot.kegboard.KegboardAuthTokenMessage;
import org.kegbot.kegboard.KegboardHelloMessage;
import org.kegbot.kegboard.KegboardMessage;
import org.kegbot.kegboard.KegboardMessageFactory;
import org.kegbot.kegboard.KegboardMeterStatusMessage;
import org.kegbot.kegboard.KegboardOnewirePresenceMessage;
import org.kegbot.kegboard.KegboardOutputStatusMessage;
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
import android.util.Log;

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

  /**
   * The system's USB service.
   */
  private UsbManager mUsbManager;

  private UsbDevice mUsbDevice;

  /**
   * The device currently in use, or {@code null}.
   */
  private UsbSerialDriver mSerialDevice;

  private ExecutorService mExecutorService;

  private Thread mThread;

  private boolean mRunning = true;

  public interface Listener {
    public void onDeviceAttached();

    public void onDeviceDetached();

    public void onAuthTokenMessage(KegboardAuthTokenMessage message);

    public void onHelloMessage(KegboardHelloMessage message);

    public void onMeterStatusMessage(KegboardMeterStatusMessage message);

    public void onOnewirePresenceMessage(KegboardOnewirePresenceMessage message);

    public void onOutputStatusMessage(KegboardOutputStatusMessage message);

    public void onTemperatureReadingMessage(KegboardTemperatureReadingMessage message);
  }

  private final Collection<Listener> mListeners = Sets.newLinkedHashSet();

  private Runnable mKegboardRunner = new Runnable() {
    private final KegboardMessageFactory mFactory = new KegboardMessageFactory();

    @Override
    public void run() {
      Log.i(TAG, "Serial runner running. Thread=" + Thread.currentThread().getName());

      final byte[] readBuffer = new byte[256];
      while (mRunning) {
        try {
          final int amtRead = mSerialDevice.read(readBuffer, 5000);
          if (amtRead < 0) {
            Log.w(TAG, "Device closed.");
            break;
          }
          if (amtRead > 0) {
            if (VERBOSE) {
              Log.d(TAG, "Read " + amtRead + " bytes from kegboard");
              Log.d(TAG, HexDump.dumpHexString(readBuffer, 0, amtRead));
            }

            final List<KegboardMessage> newMessages = mFactory.addBytes(readBuffer, amtRead);
            for (final KegboardMessage message : newMessages) {
              Log.i(TAG, "Received message: " + message);
              notifyListeners(message);
            }

            Arrays.fill(readBuffer, (byte) 0);
          }
        } catch (IOException e) {
          Log.e(TAG, "IOException reading from serial.");
          break;
        }
      }

      Log.i(TAG, "Serial runner exiting.");
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

  private void setUpUsbDevice(UsbDevice device) {
    Log.d(TAG, "Opening serial device");
    mUsbDevice = device;

    try {
      mSerialDevice.open();
      mSerialDevice.setBaudRate(115200);
    } catch (IOException e) {
      Log.e(TAG, "Error opening.", e);
    }
    /*
     * mExecutorService = Executors.newSingleThreadExecutor();
     * mExecutorService.submit(mKegboardRunner);
     */
    mThread = new Thread(mKegboardRunner);
    mThread.start();
  }

  private void removeUsbDevice() {
    // mExecutorService.shutdown();
  }

  @Override
  public void onDestroy() {
    unregisterReceiver(mUsbReceiver);
    if (mThread != null) {
      // mExecutorService.shutdown();
      mRunning = false;
    }
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
      } else if (message instanceof KegboardOnewirePresenceMessage) {
        listener.onOnewirePresenceMessage((KegboardOnewirePresenceMessage) message);
      } else if (message instanceof KegboardOutputStatusMessage) {
        listener.onOutputStatusMessage((KegboardOutputStatusMessage) message);
      } else if (message instanceof KegboardTemperatureReadingMessage) {
        listener.onTemperatureReadingMessage((KegboardTemperatureReadingMessage) message);
      }
    }
  }

}
