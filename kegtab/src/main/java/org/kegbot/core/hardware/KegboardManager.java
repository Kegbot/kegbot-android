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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.ProbeTable;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
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
import org.kegbot.proto.Models;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * Monitors a serial Kegboard device, sending updates to any attached listener.
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class KegboardManager extends BackgroundManager implements ControllerManager {

  private final String TAG = KegboardManager.class.getSimpleName();

  private static final String ACTION_USB_PERMISSION = KegboardManager.class.getCanonicalName()
      + ".ACTION_USB_PERMISSION";

  /** Default interval for scanning the USB device tree. */
  private static final long USB_REFRESH_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(5);

  private static final int MIN_FIRMWARE_VERSION = 17;

  private static final ProbeTable PROBE_TABLE = UsbSerialProber.getDefaultProbeTable();
  static {
    PROBE_TABLE.addProduct(0x03eb, 0x0436, CdcAcmSerialDriver.class);
  }

  private static final UsbSerialProber PROBER = new UsbSerialProber(PROBE_TABLE);

  private static final Pattern RELAY_PATTERN = Pattern.compile("relay(\\d+)");

  private ExecutorService mExecutorService;

  /**
   * The system's USB service.
   */
  private UsbManager mUsbManager;

  private Context mContext;

  private final AtomicBoolean mRunning = new AtomicBoolean(false);

  /** Callback interface to parent {@link HardwareManager}. */
  private final ControllerManager.Listener mListener;

  /**
   * {@link SystemClock#uptimeMillis()} value at which the USB device tree
   * will be rescanned.
   */
  private long mNextUsbRefreshUptimeMillis = Long.MIN_VALUE;

  /**
   * Maps a connected device ID to the supporting driver, or {@code null} if
   * unsupported.
   *
   * <p/>
   * This structure effectively mirrors
   * {@link android.hardware.usb.UsbManager#getDeviceList()}, containing an entry
   * for all attached devices.
   *
   * @see #findNewControllers()
   */
  @GuardedBy("this")
  private final Map<Integer, UsbSerialDriver> mConnectedDeviceToDriver = Maps.newLinkedHashMap();

  /**
   * Connected devices for which we are awaiting permission.
   */
  @GuardedBy("this")
  private final Map<Integer, UsbDevice> mConnectedDevicesNeedingPermission = Maps.newLinkedHashMap();

  /** Maps USB subsystem device IDs to open connections. */
  private final Map<UsbSerialDriver, UsbDeviceConnection> mOpenConnections = Maps
      .newLinkedHashMap();

  /** Maps active {@link UsbSerialPort}s to {@link Controller} instances. */
  private final Map<UsbSerialPort, KegboardController> mControllers = Maps.newLinkedHashMap();

  /**
   * Queue of controllers pending removal.
   * <p>
   * Controllers are added to this queue by their service thread (via
   * {@link #handleControllerError(KegboardController, Exception)}) and
   * removed in {@link #serviceControllers()}.
   * </p>
   */
  private final Queue<KegboardController> mControllerErrors = Queues.newLinkedBlockingQueue();

  private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      Log.d(TAG, "Received broadcast: " + action);
      refreshSoon();
    }
  };

  public KegboardManager(Bus bus, Context context, ControllerManager.Listener listener) {
    super(bus);
    mContext = context.getApplicationContext();
    mListener = listener;
  }

  @Override
  public synchronized void start() {
    Log.d(TAG, "Starting ...");

    mRunning.set(true);
    mExecutorService = Executors.newCachedThreadPool();

    final IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED); // never fired by framework :(
    mContext.registerReceiver(mUsbReceiver, filter);

    super.start();
  }

  @Override
  public synchronized void stop() {
    Log.d(TAG, "Stopping ...");
    mRunning.set(false);

    for (final UsbSerialDriver driver : mConnectedDeviceToDriver.values()) {
      removeDriver(driver);
    }
    mConnectedDevicesNeedingPermission.clear();

    mContext.unregisterReceiver(mUsbReceiver);
    mExecutorService.shutdown();
    mExecutorService = null;

    super.stop();
  }

  @Override
  public void refreshSoon() {
    Log.d(TAG, "refreshSoon");
    mNextUsbRefreshUptimeMillis = SystemClock.uptimeMillis();
  }

  public boolean toggleOutput(final Models.FlowToggle toggle, final boolean enable) {
    Log.d(TAG, "toggleOutput: toggle=" + toggle.getName() + " enable=" + enable);
    final String boardName = toggle.getController().getName();
    final KegboardController controller = getControllerByName(toggle.getController().getName());
    if (controller == null) {
      Log.w(TAG, "No controller with name " + boardName);
      return false;
    }

    final String portName = toggle.getPortName();
    final Matcher matcher = RELAY_PATTERN.matcher(portName);
    if (!matcher.matches()) {
      Log.w(TAG, "Unrecognized port name " + portName);
      return false;
    }
    final int portNumber = Integer.valueOf(matcher.group(1)).intValue();

    try {
      return controller.scheduleToggleOutput(portNumber, enable);
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

    Log.d(TAG, "runInBackground(): running.");

    try {
      while (mRunning.get()) {
        while (mControllerErrors.size() > 0) {
          removeSerialPort(mControllerErrors.remove().getPort());
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
      Log.d(TAG, "runInBackground(): exiting.");
    }
  }

  /**
   * Refreshes the internal list of controllers, by identifying
   * newly-added and removed controllers via
   * {@link android.hardware.usb.UsbManager#getDeviceList()}.
   */
  private synchronized void findNewControllers() {
    final Collection<UsbDevice> devices = mUsbManager.getDeviceList().values();
    final Set<Integer> connectedIds = Sets.newLinkedHashSet();

    // Call onDeviceAdded for each added device.
    for (final UsbDevice device : devices) {
      final Integer deviceId = Integer.valueOf(device.getDeviceId());
      connectedIds.add(deviceId);

      if (mConnectedDeviceToDriver.containsKey(deviceId)) {
        // We already know about this device; ignore it.
        continue;
      } else if (mConnectedDevicesNeedingPermission.containsKey(deviceId)) {
        // We're waiting for permission to use this device.
        if (!mUsbManager.hasPermission(device)) {
          continue;
        }
      }

      if (!mUsbManager.hasPermission(device)) {
        Log.i(TAG, "No permission for device: " + deviceId);
        onNeedDevicePermission(device);
      } else {
        mConnectedDevicesNeedingPermission.remove(deviceId);
        onDeviceAdded(device);
      }
    }

    final Set<Integer> removedIds = Sets.newLinkedHashSet(mConnectedDeviceToDriver.keySet());
    removedIds.removeAll(connectedIds);

    // Call onDeviceRemoved for each removed device.
    for (final Integer deviceId : removedIds) {
      onDeviceRemoved(deviceId);
    }
  }

  private synchronized void onNeedDevicePermission(UsbDevice device) {
    // We use an intent-filter to capture devices, so no need to request
    // permission: Android will do it for us.
    mConnectedDevicesNeedingPermission.put(Integer.valueOf(device.getDeviceId()), device);
  }

  /**
   * Called when a new {@link UsbDevice} has been detected on the system.
   * This method will establish whether the device is supported, by finding a
   * compatible UsbSerialDriver.
   * <p/>
   * This method always creates an entry in {@link #mConnectedDeviceToDriver},
   * with key matching the {@link android.hardware.usb.UsbDevice UsbDevice's}
   * device ID.
   *
   * @param device the newly-detected device.
   */
  private synchronized void onDeviceAdded(UsbDevice device) {
    final Integer deviceId = Integer.valueOf(device.getDeviceId());
    Preconditions.checkArgument(!mConnectedDeviceToDriver.containsKey(deviceId),
        "BUG: onDeviceAdded called with already-added device");

    Log.i(TAG, "onDeviceAdded: " + deviceId);
    final UsbSerialDriver driver = PROBER.probeDevice(device);
    mConnectedDeviceToDriver.put(Integer.valueOf(device.getDeviceId()), driver);

    if (driver == null) {
      Log.d(TAG, "  No drivers support this device.");
      return;
    }

    Log.d(TAG, "  Driver match: " + driver);
    addDriver(driver);
  }

  private synchronized void addDriver(UsbSerialDriver driver) {
    final UsbDevice device = driver.getDevice();

    if (!mUsbManager.hasPermission(device)) {
      Log.d(TAG, "No permission for device: " + device);
      return;
    }
    final UsbDeviceConnection connection = mUsbManager.openDevice(device);
    if (connection == null) {
      Log.w(TAG, "Could not open connection to device: " + device);
      return;
    }
    mOpenConnections.put(driver, connection);

    for (final UsbSerialPort port : driver.getPorts()) {
      addSerialPort(connection, port);
    }
  }

  private synchronized void addSerialPort(final UsbDeviceConnection connection, final UsbSerialPort port) {
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
      status = Controller.STATUS_UNRESPONSIVE;
    } else if (verified.getFirmwareVersion() < MIN_FIRMWARE_VERSION) {
      // Board firmware too old.
      status = Controller.STATUS_NEED_UPDATE;
    } else if (Strings.isNullOrEmpty(verified.getSerialNumber())) {
      // Board needs a serial number.  OK for now.
      status = Controller.STATUS_OK;
    } else {
      // All good! This baby's ready to report!
      status = Controller.STATUS_OK;
    }

    for (final KegboardController existingController : mControllers.values()) {
      if (existingController.getName().equals(controller.getName())) {
        Log.w(TAG, "Already have a controller named " + controller.getName());
        status = Controller.STATUS_NAME_CONFLICT;
        break;
      }
    }

    Log.d(TAG, "addSerialPort: setting controller status " + status);
    controller.setStatus(status);
    mControllers.put(port, controller);

    if (Controller.STATUS_OK.equals(status)) {
      mListener.onControllerAttached(controller);
    }
  }

  private synchronized void onDeviceRemoved(final Integer deviceId) {
    Log.i(TAG, "onDeviceRemoved: " + deviceId);
    final UsbSerialDriver driver = mConnectedDeviceToDriver.remove(deviceId);
    if (driver != null) {
      removeDriver(driver);
      final UsbDeviceConnection connection = mOpenConnections.remove(driver);
      if (connection != null) {
        connection.close();
      }
    }
  }

  private synchronized void removeDriver(final UsbSerialDriver driver) {
    Log.d(TAG, "-- Removing driver " + driver);
    for (final UsbSerialPort port : driver.getPorts()) {
      removeSerialPort(port);
    }
    mOpenConnections.remove(driver);
  }

  private synchronized void removeSerialPort(final UsbSerialPort port) {
    Log.d(TAG, "--- Removing port " + port);
    final KegboardController controller = mControllers.get(port);
    if (controller != null) {
      removeController(controller);
    }
    closePort(port);
  }

  private synchronized void removeController(final KegboardController controller) {
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
    Log.d(TAG, "verifyFirmware: Pinging controller: " + controller);

    final KegboardHelloMessage pingResponse = pingController(controller);
    if (pingResponse == null) {
      Log.w(TAG, "verifyFirmware: No response from controller: " + controller);
      return null;
    }

    if (!Strings.isNullOrEmpty(pingResponse.getSerialNumber())) {
      Log.d(TAG, "verifyFirmware: Success: " + pingResponse);
      return pingResponse;
    }

    Log.d(TAG, "verifyFirmware: Board has no serial number, setting...");
    return setControllerSerialNumber(controller);
  }

  private KegboardHelloMessage pingController(KegboardController controller) throws IOException {
    final int maxAttempts = 4;

    KegboardHelloMessage helloMessage = null;
    for (int i=0; i < maxAttempts; i++) {
      SystemClock.sleep(200);
      controller.ping();
      SystemClock.sleep(500);

      for (final KegboardMessage message : controller.readMessages()) {
        if (message instanceof KegboardHelloMessage) {
          return (KegboardHelloMessage) message;
        }
      }
    }

    return null;
  }

  private KegboardHelloMessage setControllerSerialNumber(final KegboardController controller) throws IOException {
    final int randInt = new SecureRandom().nextInt();
    final String randStr = HexDump.toHexString(randInt);
    assert randStr.length() == 8;
    final String serialNumber = String.format("KB-0000-0000-%s", randStr);

    Log.i(TAG, "Setting serial number: " + serialNumber);
    controller.setSerialNumber(serialNumber);
    return pingController(controller);
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
        final int deviceId = controller.getPort().getDriver().getDevice().getDeviceId();
        final int portNumber = controller.getPort().getPortNumber();
        final String threadName = String.format("kegboard-thr:%s-%s", deviceId, portNumber);
        Thread.currentThread().setName(threadName);

        try {
          final String controllerName = String.valueOf(controller);
          Log.d(TAG, String.format("Thread %s servicing controller %s", threadName, controllerName));
          while (mRunning.get()) {
            try {
              controller.blockingRead();
            } catch (IOException e) {
              handleControllerError(controller, e);
              Log.d(TAG, String.format("Thread %s finishing, controller closed: %s",
                  threadName, controllerName));
              return;
            }
          }
        } catch (Throwable t) {
          Log.wtf(TAG, String.format("Uncaught exception in thread %s: %s", threadName, t), t);
          throw new RuntimeException(t);
        }
      }
    });
  }

  private boolean serviceControllers() {
    boolean didIo = false;
    for (final KegboardController controller : mControllers.values()) {
      if (!Controller.STATUS_OK.equals(controller.getStatus())) {
        continue;
      }

      //Log.d(TAG, "Service controller " + controller);
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

      final String meterName = String.format("%s.%s", controller.getName(), meterStatus.getMeterName());
      final FlowMeter meter = controller.getFlowMeter(meterName);
      if (meter != null) {
        controllerEvent = new MeterUpdateEvent(meter);
      }
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

  private void handleControllerError(final KegboardController controller, @Nullable final Exception e) {
    Log.w(TAG, String.format("Marking controller %s disabled to error: %s", controller, e));
    controller.setStatus(Controller.STATUS_OPEN_ERROR);
    mControllerErrors.add(controller);
  }

  @Override
  public synchronized void dump(IndentingPrintWriter writer) {
    if (mControllers.isEmpty()) {
      writer.println("Controllers: none.");
    } else {
      writer.println("Controllers: ");
      writer.increaseIndent();
      int i = 1;
      for (final KegboardController controller : mControllers.values()) {
        writer.print(i++);
        writer.print(": ");
        writer.println(controller);
      }
      writer.decreaseIndent();
      writer.println();
    }

    if (mConnectedDevicesNeedingPermission.isEmpty()) {
      writer.println("Devices needing permission: none.");
    } else {
      writer.println("Devices needing permission: ");
      writer.increaseIndent();
      int i = 1;
      for (final Map.Entry<Integer, UsbDevice> entry : mConnectedDevicesNeedingPermission.entrySet()) {
        writer.print(i++);
        writer.print(": ");
        writer.println(entry.getValue());
      }
      writer.decreaseIndent();
      writer.println();
    }
  }

}
