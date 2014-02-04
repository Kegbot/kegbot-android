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

package org.kegbot.core.hardware;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.SystemClock;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.squareup.otto.Bus;

import org.kegbot.app.event.Event;
import org.kegbot.app.util.IndentingPrintWriter;
import org.kegbot.core.AuthenticationToken;
import org.kegbot.core.BackgroundManager;
import org.kegbot.core.FlowMeter;
import org.kegbot.kegboard.KegboardAuthTokenMessage;
import org.kegbot.kegboard.KegboardHelloMessage;
import org.kegbot.kegboard.KegboardMessage;
import org.kegbot.kegboard.KegboardMeterStatusMessage;
import org.kegbot.kegboard.KegboardTemperatureReadingMessage;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Monitors a serial Kegboard device, sending updates to any attached listener.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegboardManager extends BackgroundManager {

  private final String TAG = KegboardManager.class.getSimpleName();

  private static final String ACTION_USB_PERMISSION = KegboardManager.class.getCanonicalName()
      + ".ACTION_USB_PERMISSION";

  private static final long USB_REFRESH_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(5);

  private static final int MIN_FIRMWARE_VERSION = 17;

  public static final String CONTROLLER_STATUS_OK = "ok";
  public static final String CONTROLLER_STATUS_NEED_UPDATE = "need-update";
  public static final String CONTROLLER_STATUS_NEED_SERIAL_NUMBER = "need-serial-number";
  public static final String CONTROLLER_STATUS_UNRESPONSIVE = "unresponsive";
  public static final String CONTROLLER_STATUS_OPEN_ERROR = "open-error";

  private static final ProbeTable PROBE_TABLE = UsbSerialProber.getDefaultProbeTable();
  static {
    PROBE_TABLE.addProduct(0x03eb, 0x0436, CdcAcmSerialDriver.class);
  }

  private static final UsbSerialProber PROBER = new UsbSerialProber(PROBE_TABLE);

  private final ExecutorService mExecutorService = Executors.newCachedThreadPool();

  /**
   * The system's USB service.
   */
  private UsbManager mUsbManager;

  private Context mContext;

  private final HardwareManager.Listener mListener;

  private long mNextUsbRefreshUptimeMillis = Long.MIN_VALUE;

  /**
   * Maps a connected device ID to the supporting driver, or {@code null} if
   * unsupported.
   */
  private final Map<Integer, UsbSerialDriver> mConnectedDeviceToDriver = Maps.newLinkedHashMap();

  /** Maps USB subsystem device IDs to open connections. */
  private final Map<UsbSerialDriver, UsbDeviceConnection> mOpenConnections = Maps
      .newLinkedHashMap();
  private final Map<UsbSerialPort, KegboardController> mControllers = Maps.newLinkedHashMap();
  private final Map<UsbSerialPort, String> mStatusByPort = Maps.newLinkedHashMap();

  private final Queue<KegboardController> mControllerErrors = Queues.newLinkedBlockingQueue();

  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      Log.d(TAG, "Received broadcast: " + action);
      refreshSoon();
    }
  };

  public KegboardManager(Bus bus, Context context, HardwareManager.Listener listener) {
    super(bus);
    mContext = context.getApplicationContext();
    mListener = listener;
  }

  @Override
  public synchronized void start() {
    Log.d(TAG, "Starting ...");

    final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED); // never fired by framework :(
    mContext.registerReceiver(mUsbReceiver, filter);

    super.start();
  }

  @Override
  public synchronized void stop() {
    Log.d(TAG, "Stopping ...");
    mContext.unregisterReceiver(mUsbReceiver);

    super.stop();
  }

  public void refreshSoon() {
    Log.d(TAG, "refreshSoon");
    mNextUsbRefreshUptimeMillis = SystemClock.uptimeMillis();
  }

  public boolean toggleOutput(final String outputName, final boolean enable) {
    int sepIndex = outputName.indexOf(':');
    if (sepIndex < 0) {
      sepIndex = outputName.indexOf('.');
    }
    if (sepIndex < 0) {
      Log.w(TAG, "Unrecognized output name, ignoring: " + outputName);
      return false;
    }
    if ((sepIndex + 1) >= outputName.length()) {
      Log.w(TAG, "Malformed output name, ignoring: " + outputName);
      return false;
    }

    // Split board and output names.
    final String boardName = outputName.substring(0, sepIndex);
    final String relayName = outputName.substring(sepIndex + 1);
    Log.d(TAG, "Enabling board=" + boardName + " relay=" + relayName);

    // Parse relay name.
    if (!relayName.startsWith("relay") || relayName.length() < 6 ||
        !Character.isDigit(relayName.charAt(5))) {
      Log.w(TAG, "Unrecognized output name: " + relayName);
      return false;
    }

    final int outputNumber = relayName.charAt(5);

    final KegboardController controller = getControllerByName(boardName);
    if (controller == null) {
      Log.w(TAG, "No controller with name " + boardName);
      return false;
    }

    try {
      return controller.scheduleToggleOutput(outputNumber, enable);
    } finally {
      refreshSoon();
    }
  }

  @Override
  protected void runInBackground() {
    mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
    Log.d(TAG, "Usb manager: " + mUsbManager);

    if (mUsbManager == null) {
      Log.e(TAG, "No USB manager, exiting.");
      return;
    }

    Log.d(TAG, "Main loop running.");

    try {
      while (true) {
        while (mControllerErrors.size() > 0) {
          removePort(mControllerErrors.remove().getPort());
        }

        final long now = SystemClock.uptimeMillis();
        if (now > mNextUsbRefreshUptimeMillis) {
          findNewControllers();
          mNextUsbRefreshUptimeMillis = SystemClock.uptimeMillis() + USB_REFRESH_INTERVAL_MILLIS;
        }

        boolean didWork = serviceControllers();

        if (Thread.currentThread().isInterrupted()) {
          Log.w(TAG, "Thread interrupted, exiting.");
          break;
        }

        if (!didWork) {
          SystemClock.sleep(100);
        }
      }
    } finally {
      Log.d(TAG, "Main loop exiting.");
    }
  }

  /**
   * Loads any new controllers.
   */
  private void findNewControllers() {
    final Collection<UsbDevice> devices = mUsbManager.getDeviceList().values();
    final Set<Integer> connectedIds = Sets.newLinkedHashSet();

    for (final UsbDevice device : devices) {
      final Integer deviceId = Integer.valueOf(device.getDeviceId());
      connectedIds.add(deviceId);

      if (mConnectedDeviceToDriver.containsKey(deviceId)) {
        // We already know about this device; ignore it.
        continue;
      }
      onDeviceAdded(device);
    }

    for (final Map.Entry<Integer, UsbSerialDriver> entry : mConnectedDeviceToDriver.entrySet()) {
      final Integer deviceId = entry.getKey();
      final UsbSerialDriver driver = entry.getValue();
      if (connectedIds.contains(deviceId)) {
        continue; // still here
      }
      if (driver != null) {
        removeDevice(driver.getDevice());
      }
    }
  }

  /**
   * Called when a new {@link UsbDevice} has been detected on the system.
   *
   * @param device the newly-detected device.
   */
  private void onDeviceAdded(UsbDevice device) {
    final Integer deviceId = Integer.valueOf(device.getDeviceId());
    if (mConnectedDeviceToDriver.containsKey(deviceId)) {
      Log.wtf(TAG, "Device already known?!");
      return;
    }

    Log.d(TAG, "Probing newly-added UsbDevice: " + device);

    final UsbSerialDriver driver = PROBER.probeDevice(device);
    mConnectedDeviceToDriver.put(Integer.valueOf(device.getDeviceId()), driver);

    if (driver == null) {
      Log.d(TAG, "  No drivers support this device.");
      return;
    }

    Log.d(TAG, "  Driver match: " + driver);
    onDriverAdded(driver);
  }

  private void onDriverAdded(UsbSerialDriver driver) {
    final UsbDevice device = driver.getDevice();

    final UsbDeviceConnection connection = mUsbManager.openDevice(device);
    if (connection == null) {
      Log.w(TAG, "Could not open connection to device: " + device);
      return;
    }
    mOpenConnections.put(driver, connection);

    for (final UsbSerialPort port : driver.getPorts()) {
      onSerialPortAdded(connection, port);
    }
  }

  private void onSerialPortAdded(final UsbDeviceConnection connection, final UsbSerialPort port) {
    final KegboardController controller;
    final KegboardHelloMessage verified;
    try {
      port.open(connection);
      port.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1,
          UsbSerialPort.FLOWCONTROL_NONE);
      port.setDTR(true);

      controller = new KegboardController(port);
      startServiceThreadForController(controller);

      verified = verifyFirmware(controller);
    } catch (IOException e) {
      try {
        port.close();
      } catch (IOException e2) {
        // Close quietly.
      }
      return;
    }

    String status;
    if (verified == null) {
      // Board is unresponsive.
      status = CONTROLLER_STATUS_UNRESPONSIVE;
    } else if (verified.getFirmwareVersion() < MIN_FIRMWARE_VERSION) {
      // Board firmware too old.
      status = CONTROLLER_STATUS_NEED_UPDATE;
    } else if (Strings.isNullOrEmpty(verified.getSerialNumber())) {
      // Board needs a serial number.
      status = CONTROLLER_STATUS_NEED_SERIAL_NUMBER;
    } else {
      // All good! This baby's ready to report!
      status = CONTROLLER_STATUS_OK;
    }
    mStatusByPort.put(port, status);

    if (CONTROLLER_STATUS_OK.equals(status)) {
      mControllers.put(port, controller);
      mListener.onControllerAttached(controller);
    }
  }

  private void removeDevice(final UsbDevice device) {
    Log.d(TAG, "- Removing device " + device);
    final UsbSerialDriver driver =
        mConnectedDeviceToDriver.remove(Integer.valueOf(device.getDeviceId()));
    if (driver != null) {
      removeDriver(driver);
      final UsbDeviceConnection connection = mOpenConnections.remove(driver);
      if (connection != null) {
        connection.close();
      }
    }
  }

  private void removeDriver(final UsbSerialDriver driver) {
    Log.d(TAG, "-- Removing driver " + driver);
    for (final UsbSerialPort port : driver.getPorts()) {
      removePort(port);
    }
    mOpenConnections.remove(driver);
  }

  private void removePort(final UsbSerialPort port) {
    Log.d(TAG, "--- Removing port " + port);
    final KegboardController controller = mControllers.get(port);
    if (controller != null) {
      removeController(controller);
    }
    mStatusByPort.remove(port);
    closePort(port);
  }

  private void removeController(final KegboardController controller) {
    Log.d(TAG, "---- Removing controller " + controller);
    mControllers.remove(controller.getPort());
    mListener.onControllerRemoved(controller);
  }

  /** Quietly closes the given {@link UsbSerialPort}. */
  private void closePort(UsbSerialPort port) {
    try {
      port.close();
    } catch (IOException e) {
      Log.d(TAG, "Error closing port, ignoring..");
    }
  }

  private KegboardHelloMessage verifyFirmware(KegboardController controller) throws IOException {
    Log.d(TAG, "Pinging controller: " + controller);

    for (int attempt = 0; attempt < 4; attempt++) {
      SystemClock.sleep(500);
      controller.ping();
      SystemClock.sleep(200);

      for (final KegboardMessage message : controller.readMessages()) {
        if (message instanceof KegboardHelloMessage) {
          return (KegboardHelloMessage) message;
        }
      }
    }

    Log.w(TAG, "No response to ping, disabling board.");
    return null;
  }

  private KegboardController getControllerByName(final String name) {
    for (final KegboardController controller : mControllers.values()) {
      if (name.equals(controller.getName())) {
        return controller;
      }
    }
    return null;
  }

  private void startServiceThreadForController(final KegboardController controller) {
    mExecutorService.submit(new Runnable() {
      @Override
      public void run() {
        final String threadName = Thread.currentThread().getName();
        final String controllerName = String.valueOf(controller);
        Log.d(TAG, String.format("Thread %s servicing controller %s", threadName, controllerName));
        while (true) {
          try {
            controller.blockingRead();
          } catch (IOException e) {
            handleControllerError(controller, e);
            Log.d(TAG, String.format("Thread %s finishing, controller closed: %s",
                threadName, controllerName));
            return;
          }
        }
      }
    });
  }

  private boolean serviceControllers() {
    boolean didIo = false;
    for (final KegboardController controller : mControllers.values()) {
      if (!CONTROLLER_STATUS_OK.equals(mStatusByPort.get(controller.getPort()))) {
        continue;
      }

      Log.d(TAG, "Service controller " + controller);
      // Refresh all enabled outputs. Best effort.
      try {
        controller.refreshOutputs();
      } catch (IOException e) {
        handleControllerError(controller, e);
        continue;
      }

      // Consume all available messages.
      while (true) {
        final KegboardMessage message = controller.readMessage();
        if (message == null) {
          break;
        }
        handleControllerMessage(controller, message);
        didIo = true;
      }
    }
    return didIo;
  }

  private void handleControllerMessage(final KegboardController controller,
      final KegboardMessage message) {
    Log.d(TAG, String.format("Handling message: %s", message));

    Event controllerEvent = null;

    if (message instanceof KegboardHelloMessage) {
      // TODO
    } else if (message instanceof KegboardMeterStatusMessage) {
      final KegboardMeterStatusMessage meterStatus = (KegboardMeterStatusMessage) message;

      final String meterName = meterStatus.getMeterName();
      final FlowMeter meter = controller.getFlowMeter(meterName);
      controllerEvent = new MeterUpdateEvent(meter);
    } else if (message instanceof KegboardTemperatureReadingMessage) {
      final KegboardTemperatureReadingMessage tempMessage =
          (KegboardTemperatureReadingMessage) message;

      final String sensorName = tempMessage.getName();
      controllerEvent = new ThermoSensorUpdateEvent(controller.getThermoSensor(sensorName));
    } else if (message instanceof KegboardAuthTokenMessage) {
      final KegboardAuthTokenMessage authMessage = (KegboardAuthTokenMessage) message;

      final AuthenticationToken token =
          new AuthenticationToken(authMessage.getName(), authMessage.getToken());
      if (authMessage.getStatus() == KegboardAuthTokenMessage.Status.PRESENT) {
        controllerEvent = new TokenAttachedEvent(token);
      } else {
        controllerEvent = new TokenDetachedEvent(token);
      }
    } else {
      Log.w(TAG, "Unhandled message: " + message);
    }
    if (controllerEvent != null) {
      mListener.onControllerEvent(controller, controllerEvent);
    }
  }

  private void handleControllerError(final KegboardController controller, final Exception e) {
    Log.w(TAG, String.format("Marking controller %s disabled to error: %s", controller, e));
    mStatusByPort.put(controller.getPort(), CONTROLLER_STATUS_OPEN_ERROR);
    mControllerErrors.add(controller);
  }

  @Override
  protected void dump(IndentingPrintWriter writer) {
    writer.println("# Port Status:\n");
    writer.increaseIndent();
    for (final Map.Entry<UsbSerialPort, String> entry : mStatusByPort.entrySet()) {
      writer.printPair(String.valueOf(entry.getKey()), entry.getValue()).println();
    }
    writer.decreaseIndent();
    writer.println();

    writer.printPair("numControllers", Integer.valueOf(mControllers.size())).println();
    for (final KegboardController controller : mControllers.values()) {
      writer.printf("  %s", controller).println();
    }
  }

}
