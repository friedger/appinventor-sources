// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.BluetoothReflection;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.SdkLevel;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SensorTag component
 *
 * @author friedger@novoda.com (Friedger Müffke)
 */
@DesignerComponent(version = YaVersion.SENSOR_TAG_COMPONENT_VERSION,
    description = "SensorTag component",
    category = ComponentCategory.CONNECTIVITY,
    nonVisible = true,
    iconName = "images/bluetooth.png")
@SimpleObject
@UsesPermissions(permissionNames =
                 "android.permission.BLUETOOTH, " +
                 "android.permission.BLUETOOTH_ADMIN")
public final class SensorTag extends BluetoothConnectionBase {
  private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

  private final Handler androidUIHandler;

  /**
   * Creates a new BluetoothServer.
   */
  public SensorTag(ComponentContainer container) {
    super(container, "SensorTag");
    androidUIHandler = new Handler();
  }

  /**
   * Start scanning the environment for SensorTags.
   */
  @SimpleFunction(description = "Start scanning the environment for SensorTags ")
  public void FindSensorTags() {

    final Object bluetoothAdapter = BluetoothReflection.getBluetoothAdapter();
    if (bluetoothAdapter == null) {
      form.dispatchErrorOccurredEvent(this, functionName,
          ErrorMessages.ERROR_BLUETOOTH_NOT_AVAILABLE);
      return;
    }

    if (!BluetoothReflection.isBluetoothEnabled(bluetoothAdapter)) {
      form.dispatchErrorOccurredEvent(this, functionName,
          ErrorMessages.ERROR_BLUETOOTH_NOT_ENABLED);
      return;
    }

    UUID uuid;
    try {
      uuid = UUID.fromString(uuidString);
    } catch (IllegalArgumentException e) {
      form.dispatchErrorOccurredEvent(this, functionName,
          ErrorMessages.ERROR_BLUETOOTH_INVALID_UUID, uuidString);
      return;
    }

    try {
      if (!secure && SdkLevel.getLevel() >= SdkLevel.LEVEL_) {
        // listenUsingInsecureRfcommWithServiceRecord was introduced in level 10
        bluetoothServerSocket = BluetoothReflection.listenUsingInsecureRfcommWithServiceRecord(
            bluetoothAdapter, name, uuid);
      } else {
        bluetoothServerSocket = BluetoothReflection.listenUsingRfcommWithServiceRecord(
            bluetoothAdapter, name, uuid);
      }
      arBluetoothServerSocket.set(bluetoothServerSocket);
    } catch (IOException e) {
      form.dispatchErrorOccurredEvent(this, functionName,
          ErrorMessages.ERROR_BLUETOOTH_UNABLE_TO_LISTEN);
      return;
    }

    AsynchUtil.runAsynchronously(new Runnable() {
      public void run() {
        Object acceptedBluetoothSocket = null;

        Object bluetoothServerSocket = arBluetoothServerSocket.get();
        if (bluetoothServerSocket != null) {
          try {
            try {
              acceptedBluetoothSocket = BluetoothReflection.accept(bluetoothServerSocket);
            } catch (IOException e) {
              androidUIHandler.post(new Runnable() {
                public void run() {
                  form.dispatchErrorOccurredEvent(BluetoothServer.this, functionName,
                      ErrorMessages.ERROR_BLUETOOTH_UNABLE_TO_ACCEPT);
                }
              });
              return;
            }
          } finally {
            StopAccepting();
          }
        }

        if (acceptedBluetoothSocket != null) {
          // Call setConnection and signal the event on the main thread.
          final Object bluetoothSocket = acceptedBluetoothSocket;
          androidUIHandler.post(new Runnable() {
            public void run() {
              try {
                setConnection(bluetoothSocket);
              } catch (IOException e) {
                Disconnect();
                form.dispatchErrorOccurredEvent(BluetoothServer.this, functionName,
                    ErrorMessages.ERROR_BLUETOOTH_UNABLE_TO_ACCEPT);
                return;
              }

              ConnectionAccepted();
            }
          });
        }
      }
    });
  }

  /**
   * Returns true if this BluetoothServer component is accepting an
   * incoming connection.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public final boolean IsAccepting() {
    return (arBluetoothServerSocket.get() != null);
  }

  /**
   * Stop accepting an incoming connection.
   */
  @SimpleFunction(description = "Stop accepting an incoming connection.")
  public void StopAccepting() {
    Object bluetoothServerSocket = arBluetoothServerSocket.getAndSet(null);
    if (bluetoothServerSocket != null) {
      try {
        BluetoothReflection.closeBluetoothServerSocket(bluetoothServerSocket);
      } catch (IOException e) {
        Log.w(logTag, "Error while closing bluetooth server socket: " + e.getMessage());
      }
    }
  }

  /**
   * Indicates that a bluetooth connection has been accepted.
   */
  @SimpleEvent(description = "Indicates that a bluetooth connection has been accepted.")
  public void ConnectionAccepted() {
    Log.i(logTag, "Successfullly accepted bluetooth connection.");
    EventDispatcher.dispatchEvent(this, "ConnectionAccepted");
  }
}
