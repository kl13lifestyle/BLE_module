package com.example.juggernaut.ble_module.BLEUtils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import com.example.juggernaut.ble_module.MainActivity;
import com.example.juggernaut.ble_module.notificationServer;

import java.util.Arrays;

import static android.content.ContentValues.TAG;

public class BluetoothServerInstance {
    private static BluetoothManager bluetoothManager;
    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private static BluetoothGattServer bluetoothGattServer;
    private static BluetoothDevice bluetoothDevice;
    private static BluetoothGattService service;

    private static BluetoothGattCharacteristic PatternCharacteristic;
    private static BluetoothGattCharacteristic NotificationCharacteristic;

    private static BluetoothGattDescriptor controlDescriptor;

    private static Context context;

    private static byte[] value;

    static Activity activity;
    public  static void initialize_bluetooth() {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        bluetoothGattServer = bluetoothManager.openGattServer(BluetoothServerInstance.context, bluetoothGattServerCallback);
        if (bluetoothGattServer == null) {
            Log.i(TAG, "Not able to connect");
            ensureBleFeaturesAvailable();
        } else {
            Log.i(TAG, "Able to connect");
        }
    }

    private static void ensureBleFeaturesAvailable() {
        if (bluetoothAdapter == null) {
            Toast.makeText(BluetoothServerInstance.context, "bluetoothNotSupported", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Bluetooth not supported");
        } else if (!bluetoothAdapter.isEnabled()) {
            // Make sure bluetooth is enabled.
            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //MainActivity.startActivityForResult(enableBtIntent, 1);
        }
    }

    public static void initServer() {
        service = new BluetoothGattService(Constants.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        PatternCharacteristic = new BluetoothGattCharacteristic(Constants.MATRIX_UUID, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
        NotificationCharacteristic = new BluetoothGattCharacteristic(Constants.COMMUNICATION_NOTIFICATION_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
        controlDescriptor = new BluetoothGattDescriptor(Constants.CONTROL_DESC_UUID,BluetoothGattDescriptor.PERMISSION_READ|BluetoothGattDescriptor.PERMISSION_WRITE);
        controlDescriptor.setValue(new byte[] {0,0});
        PatternCharacteristic.setValue(new byte[] {0,0});
        PatternCharacteristic.addDescriptor(controlDescriptor);
        service.addCharacteristic(PatternCharacteristic);
        service.addCharacteristic(NotificationCharacteristic);
        bluetoothGattServer.addService(service);
    }

    public static void startAdvertising() {
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED).setConnectable(true).setTimeout(0).setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW).build();
        AdvertiseData advertiseData = new AdvertiseData.Builder().setIncludeDeviceName(true).addServiceUuid(new ParcelUuid(Constants.SERVICE_UUID)).build();
        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
    }

    static AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(TAG, "Advertisement Started "+settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.i(TAG, "Advertisement Failed "+errorCode);
        }
    };
    static BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            BluetoothServerInstance.bluetoothDevice = device;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected :"+status);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected:"+status);
            }
            else {
                Log.i(TAG, "onConnectionStateChange: "+status+" "+newState);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.i(TAG, "onCharacteristicReadRequest: "+Arrays.toString(characteristic.getValue()));
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristic.getValue());
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.i(TAG, "onDescriptorReadRequest: "+ Arrays.toString(descriptor.getValue()));
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, descriptor.getValue());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            Log.i(TAG, "onDescriptorWriteRequest: "+Arrays.toString(value));
            controlDescriptor.setValue(value);
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.i(TAG, "onNotificationSent: "+status);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.i(TAG,"Characteristic value written : "+ Arrays.toString(value));
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            MainActivity.notificationType(value[0]);
        }
    };
    public static void setContext(Context context) {
        BluetoothServerInstance.context = context;
    }
    public static void setValue(byte[] value) {
        BluetoothServerInstance.value = value;
        PatternCharacteristic.setValue(value);
        bluetoothGattServer.notifyCharacteristicChanged(bluetoothDevice,PatternCharacteristic,true);
    }
}
