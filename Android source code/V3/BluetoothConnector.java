package co.adametaverse.blueunity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;


import com.unity3d.player.UnityPlayer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnector {

    private static BluetoothConnector mInstance = null;

    private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static BluetoothManager mBluetoothManager = null;
    private static BluetoothAdapter mBluetoothAdapter = null;
    private static BluetoothLeScanner mBluetoothLeScanner = null;
    private static DeviceListAdapter mDeviceListAdapter;
    private static boolean scanning = false;
    private static final Handler handler = new Handler();
    private static final long SCAN_PERIOD = 10000;
    private static BluetoothSocket skt;
    private static InputStream inputStream;
    private static OutputStream outputStream;
    private static BufferedReader reader;
    private static BluetoothLeService bluetoothLeService;
    private static Map<String, BluetoothGatt> gatts = new HashMap();

    private static ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothLeService = null;
        }
    };

    private static ScanCallback leScanCallback = new ScanCallback() {
        @Override
        @SuppressLint("MissingPermission")
        public void onScanResult(int callbackType, ScanResult result) {
//            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (mDeviceListAdapter.AddDevice(device)) {
                String deviceName = device.getName() == null ? "null" : device.getName();
                String deviceAddress = device.getAddress();
                UnityPlayer.UnitySendMessage("BluetoothManager", "NewDeviceFound", deviceAddress + "+" + deviceName);
            }
        }
    };

    private static BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        @SuppressLint("MissingPermission")
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatts.put(gatt.getDevice().getAddress(), gatt);
                UnityPlayer.UnitySendMessage("BluetoothManager", "ConnectionStatus", "connected");
                gatt.discoverServices();
                // successfully connected to the GATT Server
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                gatts.remove(gatt.getDevice().getAddress());
                UnityPlayer.UnitySendMessage("BluetoothManager", "ConnectionStatus", "disconnected");
            }else if (newState == BluetoothProfile.STATE_CONNECTING){
                UnityPlayer.UnitySendMessage("BluetoothManager", "ConnectionStatus", "CONNECTING 2");
            }else if (newState == BluetoothProfile.STATE_DISCONNECTING){
                UnityPlayer.UnitySendMessage("BluetoothManager", "ConnectionStatus", "disconnecting");
            }

        }

        @Override
        @SuppressLint("MissingPermission")
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt.getServices().forEach(service -> UnityPlayer.UnitySendMessage("BluetoothManager", "Services", service.getUuid().toString()));

            }
        }
    };

    @SuppressLint("MissingPermission")
    public static BluetoothConnector getInstance() {
        if (mInstance == null)
            mInstance = new BluetoothConnector();
        return mInstance;
    }

    @SuppressLint("MissingPermission")
    public BluetoothConnector() {
//        checkPermissions(UnityPlayer.currentActivity.getApplicationContext(), UnityPlayer.currentActivity);
        mBluetoothManager = UnityPlayer.currentActivity.getApplication().getApplicationContext().getSystemService(BluetoothManager.class);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            UnityPlayer.currentActivity.startActivityForResult(enableBtIntent, 1);
        }

        if (!mBluetoothAdapter.isEnabled())
            mBluetoothAdapter.enable();

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mDeviceListAdapter = new DeviceListAdapter();


//
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(BluetoothDevice.ACTION_FOUND);
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

//        BroadcastReceiver receiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                String action = intent.getAction();
//                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                    if (mDeviceListAdapter.AddDevice(device)){
//                        String deviceName = device.getName()==null? "null": device.getName();
//                        String deviceAddress = device.getAddress();
//                        UnityPlayer.UnitySendMessage("BluetoothManager", "NewDeviceFound", deviceAddress + "+" + deviceName);
//                    }
//                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
//                    UnityPlayer.UnitySendMessage("BluetoothManager", "ScanStatus", "started");
//                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//                    UnityPlayer.UnitySendMessage("BluetoothManager", "ScanStatus", "stopped");
//                }
//            }
//        };
//        UnityPlayer.currentActivity.getApplication().getApplicationContext().registerReceiver(receiver, filter);
    }

    @SuppressLint("MissingPermission")
    private static void StartScanDevices() {
        // Check if discovery is already in progress, and cancel if so
//        if (mBluetoothAdapter.isDiscovering()) {
//            mBluetoothAdapter.cancelDiscovery();
//        }
//        mDeviceListAdapter.clearAll();

//        mBluetoothAdapter.startDiscovery();
//        Intent aaaa = new Intent(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//        UnityPlayer.currentActivity.startActivityForResult(aaaa, 1);
        ScanFilter filter = new ScanFilter.Builder().build();
        ScanSettings settings = new ScanSettings.Builder().setLegacy(false).setScanMode(2).build();
        mBluetoothLeScanner.startScan(Arrays.asList(new ScanFilter[]{filter}), settings, leScanCallback);
    }



    @SuppressLint("MissingPermission")
    private static void StopScanDevices() {
        // Check if discovery is already in progress, and cancel if so
        if (mBluetoothAdapter.isDiscovering()) {
//            mBluetoothAdapter.cancelDiscovery();
            mBluetoothLeScanner.stopScan(leScanCallback);
            UnityPlayer.UnitySendMessage("BluetoothManager", "ScanStatus", "stopped");
        }
    }

    @SuppressLint("MissingPermission")
    public static String[] GetPairedDevices() {
        try {
            ArrayList<String> paired_devices = new ArrayList<>();

            Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
            for(BluetoothDevice device:devices)
            {
                paired_devices.add(device.getAddress()+"+"+device.getName());
            }
            return paired_devices.toArray(new String[0]);
        }
        catch (Exception ex){
            UnityPlayer.UnitySendMessage("BluetoothManager", "ReadLog",  ex.toString());
            return null;
        }
    }

    @SuppressLint("MissingPermission")
    public static void StartConnection(String DeviceAdd){

        try{
            if(!BluetoothAdapter.checkBluetoothAddress(DeviceAdd))
                UnityPlayer.UnitySendMessage("BluetoothManager", "ConnectionStatus", "Device not found");
            UnityPlayer.UnitySendMessage("BluetoothManager", "ConnectionStatus", "connecting");
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(DeviceAdd);

            device.connectGatt(UnityPlayer.currentContext, true, gattCallback);
            mBluetoothLeScanner.stopScan(leScanCallback);

//            skt = device.createInsecureRfcommSocketToServiceRecord(mUUID);
//            skt = device.createRfcommSocketToServiceRecord(mUUID);
//            mBluetoothAdapter.cancelDiscovery();

//            skt.connect();
//            inputStream = skt.getInputStream();
//            outputStream = skt.getOutputStream();
//
//            reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            ReadDatathread.start();


        }
        catch (Exception ex)
        {
            UnityPlayer.UnitySendMessage("BluetoothManager", "ReadLog", "StartConnection Error: "+ex);
        }
    }
    public static void StopConnection(){
        try {
            ReadDatathread.interrupt();
            if(inputStream != null) inputStream.close();
            if(outputStream != null) outputStream.close();
            if(skt != null) skt.close();
        } catch (IOException e) {
            UnityPlayer.UnitySendMessage("BluetoothManager", "ReadLog", "StopConnection Error: "+e);
        }
    }

    private static final Runnable ReadData = new Runnable() {
        public void run() {
            while (skt.isConnected() && !ReadDatathread.isInterrupted()) {
                try {
                    if (inputStream.available() > 0) {

                        UnityPlayer.UnitySendMessage("BluetoothManager", "ReadData", reader.readLine());
                    }
                } catch (IOException e) {
                    UnityPlayer.UnitySendMessage("BluetoothManager", "ReadLog", "inputStream Error: " + e);
                }
            }
            UnityPlayer.UnitySendMessage("BluetoothManager", "ConnectionStatus", "disconnected");
        }
    };

    private static final Thread ReadDatathread = new Thread(ReadData);

    @SuppressLint("MissingPermission")
    public static void WriteData(String data, String deviceAddress, String serviceUUID, String characteristicUUID) {
        if(gatts.containsKey(deviceAddress)){
            UnityPlayer.UnitySendMessage("BluetoothManager", "Services", "contains gatt");
            BluetoothGatt gatt = gatts.get(deviceAddress);
            BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
            if(service == null) return;
            UnityPlayer.UnitySendMessage("BluetoothManager", "Services", "contains service");
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
            if(characteristic == null) return;
            UnityPlayer.UnitySendMessage("BluetoothManager", "Services", "contains characteristic");
//            gatt.writeCharacteristic(characteristic, data.getBytes(), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            characteristic.setValue(data);
            gatt.writeCharacteristic(characteristic);
        }
    }

    public static void Toast(String info){
        Toast.makeText(UnityPlayer.currentActivity.getApplicationContext(), info,Toast.LENGTH_SHORT).show();
    }

}




