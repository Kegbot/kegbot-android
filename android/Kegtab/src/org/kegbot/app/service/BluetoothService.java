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
import java.io.InputStream;
import java.util.UUID;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.kegbot.app.KegtabBroadcast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.google.common.base.Strings;
import com.hoho.android.usbserial.util.HexDump;

/**
 *
 * @author mike wakerly (mike@wakerly.com)
 */
public class BluetoothService extends BackgroundService {

  private static final String TAG = BluetoothService.class.getSimpleName();

  public static final UUID KEGTAB_BT_UUID = UUID.fromString("50c93109-154d-49c0-8565-4d63abf25615");

  private boolean mQuit;

  public class LocalBinder extends Binder {
    BluetoothService getService() {
      return BluetoothService.this;
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
    mQuit = false;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mQuit = true;
  }

  @Override
  protected void runInBackground() {
    Log.d(TAG, "Running in background.");
    while (true) {
      final BluetoothAdapter adapter = getUsableAdapter();
      if (adapter == null) {
        Log.w(TAG, "No usable adapter, exiting.");
        break;
      }
      try {
        handleOneConnection(adapter);
      } catch (IOException e) {
        Log.w(TAG, "Connection failed: " + e.toString(), e);
        break;
      }
    }
  }

  private BluetoothAdapter getUsableAdapter() {
    final BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    if (adapter == null) {
      Log.w(TAG, "Bluetooth not supported.");
      return null;
    } else if (!adapter.isEnabled()) {
      Log.w(TAG, "Bluetooth is not enabled.");
      return null;
    }
    return adapter;
  }

  private void handleOneConnection(BluetoothAdapter adapter) throws IOException {
    final BluetoothServerSocket serverSocket =
        adapter.listenUsingRfcommWithServiceRecord("kegtap", KEGTAB_BT_UUID);

    boolean serverSocketClosed = false;
    try {
      BluetoothSocket socket = null;
      while (socket == null) {
        socket = serverSocket.accept();
      }
      Log.d(TAG, "Client accepted, closing server socket.");
      serverSocket.close();
      serverSocketClosed = true;
      try {
        handleConnection(socket);
      } finally {
        Log.d(TAG, "Closing client socket.");
        socket.close();
      }
    } finally {
      if (!serverSocketClosed) {
        Log.d(TAG, "Closing server socket due to abnormal exit.");
        serverSocket.close();
      }
    }
  }

  private void handleConnection(BluetoothSocket socket) throws IOException {
    final InputStream inputStream = socket.getInputStream();

    final byte buffer[] = new byte[4096];
    int nBytes;
    while (true) {
      nBytes = inputStream.read(buffer);
      Log.d(TAG, "Read " + nBytes + " bytes.");
      Log.d(TAG, HexDump.dumpHexString(buffer, 0, nBytes));
      JsonNode message = new ObjectMapper().readValue(buffer, 0, nBytes, JsonNode.class);
      Log.d(TAG, "Parsed message: " + message);
      handleMessage(message);
    }

  }

  private void handleMessage(JsonNode root) {
    final JsonNode commandNode = root.get("command");
    if (commandNode == null || !commandNode.isValueNode()) {
      Log.d(TAG, "Kegnet BT message missing command.");
      return;
    }
    final JsonNode argsNode = root.get("args");
    if (argsNode == null) {
      Log.d(TAG, "Kegnet BT message missing command.");
      return;
    }

    final String commandName = commandNode.getValueAsText();
    if (Strings.isNullOrEmpty(commandName)) {
      Log.d(TAG, "Kegnet BT message empty command.");
      return;
    }
    Log.d(TAG, "Kegnet BT message command=" + commandName);

    if (commandName.equals("tokenAuth")) {
      final JsonNode authDevice = argsNode.get("authDevice");
      final JsonNode tokenValue = argsNode.get("tokenValue");
      if (authDevice == null || !authDevice.isValueNode() || tokenValue == null || !tokenValue.isValueNode()) {
        Log.d(TAG, "Kegnet BT malformed auth command.");
        return;
      }
      final Intent intent = KegtabBroadcast.getTokenAddedIntent(authDevice.getTextValue(), tokenValue.getTextValue());
      Log.d(TAG, "Issuing broadcast: " + intent);
      sendBroadcast(intent);
    }
  }

}
