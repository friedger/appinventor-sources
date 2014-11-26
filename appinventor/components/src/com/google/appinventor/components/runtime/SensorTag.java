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
import android.bluetooth.BluetoothDevice;

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
public final class SensorTag extends AndroidNonvisibleComponent
        implements Component, Deleteable, BluetoothReflection.BleScanCallback {
  private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final String SENSOR_TAG = "Sensor Tag";
    protected final String logTag;
  private final Handler androidUIHandler;

  /**
   * Creates a new BluetoothServer.
   */
  public SensorTag(ComponentContainer container, String logTag) {
    super(container.$form());
    this.logTag = logTag;
    androidUIHandler = new Handler();
  }

  /**
   * Start scanning the environment for SensorTags.
   */
  @SimpleFunction(description = "Start scanning the environment for SensorTags ")
  public void FindSensorTags(String uuidString) {
      findSensorTags("FindSensorTags", uuidString);
  }

  private void findSensorTags(String functionName, String uuidString){
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

    if (SdkLevel.getLevel() >= SdkLevel.LEVEL_JELLYBEAN_MR2) {
       // startLeScan was introduced in level 18
       BluetoothReflection.startLeScan(bluetoothAdapter, this );
    }
  }

  /**
   * Indicates that a bluetooth connection has been accepted.
   */
  @SimpleEvent(description = "SensorTag has been found")
  public void SensorTagFound(String deviceName, String data) {
    Log.i(logTag, "Successfullly found a sensorTag.");
    EventDispatcher.dispatchEvent(this, "SensorTagFound", deviceName);
  }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void onLeScan(Object bluetoothDevice, Object rssi, Object data) {
        String deviceName = ((BluetoothDevice) bluetoothDevice).getName();
        if (SENSOR_TAG.equals(deviceName)) {

            SensorTagFound(deviceName, bytesToHex((byte[]) data));
        }
    }

    @Override
    public void onDelete() {
        //TODO prepareToDie
}
}
