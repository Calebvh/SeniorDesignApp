package com.example.caleb.seniordesignapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {


    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private BluetoothGattCharacteristic mGattCharacteristic1;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";


    private BluetoothGattCharacteristic bluetoothGattCharacteristicHM_10;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            //mBluetoothLeService.initialize();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            Log.d(TAG,"OnServiceConnected: disconnect");
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
            mBluetoothLeService.stopSelf();
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState("Connected");
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState("Disconnected");
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

                if(mGattCharacteristic1 != null){
                    if (mNotifyCharacteristic != null) {
                        mBluetoothLeService.setCharacteristicNotification(
                                mNotifyCharacteristic, false);
                        mNotifyCharacteristic = null;
                    }
                    mBluetoothLeService.readCharacteristic(mGattCharacteristic1);
                    Log.d(TAG, "I AM READING ALL BY MYSELF");
                }else {
                    Log.d(TAG, "Characteristic is NULL");
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

                //Echo back received data, with something inserted
                final byte[] rxBytes = bluetoothGattCharacteristicHM_10.getValue();
                final byte[] insertSomething = {(byte)'X'};
                byte[] txBytes = new byte[insertSomething.length + rxBytes.length];
                System.arraycopy(insertSomething, 0, txBytes, 0, insertSomething.length);
                System.arraycopy(rxBytes, 0, txBytes, insertSomething.length, rxBytes.length);

                if(bluetoothGattCharacteristicHM_10 != null){
                    bluetoothGattCharacteristicHM_10.setValue(txBytes);
                    mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
                    mBluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristicHM_10,true);
                }

            }
        }
    };

    private void clearUI() {
        MsgBox.setText("No Data");
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                Log.d(TAG,"onOptionsItems Selected: I AM CONNECTING HERE BITCH");
                mBluetoothLeService.disconnect();
                mBluetoothLeService.close();
                mBluetoothLeService.stopSelf();
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(String resourceId) {
        //runOnUiThread(new Runnable() {
        //@Override
        //public void run() {
        mConnectionState.setText(resourceId);
        //}
        //});
    }

    private void displayData(String data) {
        if (data != null) {
            MsgBox.setText(data);
        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {

        UUID UUID_HM_10 =
                UUID.fromString(GATTAttributes.HM11);

        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = "Unknown Service";
        String unknownCharaString = "Unknown Characerfsads";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, GATTAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            Log.d(TAG,"UUID SERVICES: "+ uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                if(gattCharacteristic.getUuid().toString().equals(GATTAttributes.HM11)){
                    mGattCharacteristic1 = gattCharacteristic;
                    Log.d(TAG, "SUCCESS in Grabbing Characteristic");
                }
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, GATTAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);

                //Check if it is "HM_10"
                if(uuid.equals(GATTAttributes.HM11)){
                    bluetoothGattCharacteristicHM_10 = gattService.getCharacteristic(UUID_HM_10);
                }
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    /** ==============================================================================*/
    Button Bluetooth_Btn;
    Button Find_Btn;
    BluetoothAdapter myBluetoothAdapter;

    private static final String TAG = MainActivity.class.getName();

    DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;
    TextView MsgBox;

    /** ==============================================================================*/
    public ArrayList<BluetoothDevice> BluetoothDevices = new ArrayList<>();

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) ON , Turning On,, OFF, Turning off
     */
    private final BroadcastReceiver myBroadcastReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if(action.equals(myBluetoothAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, myBluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };


    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver myBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };

    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                if(device.getName()!= null && device.getName().equals("SABR")){
                    BluetoothDevices.add(device);

                    Log.d(TAG, "SABR Found: " + device.getName() + ": " + device.getAddress());

                    /*final Intent intent1 = new Intent(context, DevicePairing.class);
                    intent1.putExtra(DevicePairing.EXTRAS_DEVICE_NAME, device.getName());
                    intent1.putExtra(DevicePairing.EXTRAS_DEVICE_ADDRESS, device.getAddress());

                    startActivity(intent1);*/
                    Log.d(TAG, "onItemClick: deviceName = " + device.getName());
                    Log.d(TAG, "onItemClick: deviceAddress = " + device.getAddress());


                    mDeviceName = device.getName();
                    mDeviceAddress = device.getAddress();

                    Log.d(TAG, "HERE I AM in makeshift pairing");
                    Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
                    bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

                }

                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, BluetoothDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bluetooth_Btn = (Button)findViewById(R.id.BluetoothBtn);
        Find_Btn = (Button) findViewById(R.id.Find_Btn);

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        myBluetoothAdapter = bluetoothManager.getAdapter();

        //myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        lvNewDevices = (ListView) findViewById(R.id._listView);
        MsgBox = (TextView) findViewById(R.id.MsgBox);
        mConnectionState = (TextView) findViewById(R.id.connect_box);


        // Checks if Bluetooth is supported on the device.
        if (myBluetoothAdapter == null) {
            Toast.makeText(this, "Error: Bluetooth not Supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        lvNewDevices.setOnItemClickListener(MainActivity.this);


        Bluetooth_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!myBluetoothAdapter.isEnabled()){
                    TurnOn();
                }
                else{
                    TurnOff();
                }
            }
        });

        Find_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

                if(myBluetoothAdapter.isDiscovering()){
                    myBluetoothAdapter.cancelDiscovery();
                    Log.d(TAG, "btnDiscover: Canceling discovery.");

                    //check BT permissions in manifest
                    checkBTPermissions();

                    myBluetoothAdapter.startDiscovery();
                    IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
                }
                if(!myBluetoothAdapter.isDiscovering()){

                    //check BT permissions in manifest
                    checkBTPermissions();

                    myBluetoothAdapter.startDiscovery();
                    IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
                }
            }
        });
        //Green #78AB46

        final MediaPlayer AlarmMediaPLayer = MediaPlayer.create(this, R.raw.alarm_sound_2);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0);

        final Button PlayButton = (Button) this.findViewById(R.id.connect_Btn);
        PlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //for(int x = 0; x<5; x++ ){
                AlarmMediaPLayer.start();

                //}

            }
        });


    }


    public void TurnOn(){
        Bluetooth_Btn.setText("Bluetooth : ON");
        Bluetooth_Btn.setBackgroundColor(Color.GREEN);

        if(myBluetoothAdapter == null){
            Log.d(TAG, "Phone does not have bluetooth capabilities");
        }
        //Enable Bluetooth
        Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(enableBTIntent);

        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(myBroadcastReceiver1, BTIntent);

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter = new IntentFilter(myBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(myBroadcastReceiver2,intentFilter);

    }

    public void TurnOff(){
        Bluetooth_Btn.setText("Bluetooth : OFF");
        Bluetooth_Btn.setBackgroundColor(Color.RED);

        if(myBluetoothAdapter.isDiscovering()){myBluetoothAdapter.cancelDiscovery();}
        if(myBluetoothAdapter.isEnabled()){myBluetoothAdapter.disable();}

    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
        //first cancel discovery because its very memory intensive.
        myBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = BluetoothDevices.get(i).getName();
        String deviceAddress = BluetoothDevices.get(i).getAddress();

        final Intent intent = new Intent(this, DevicePairing.class);
        intent.putExtra(DevicePairing.EXTRAS_DEVICE_NAME, BluetoothDevices.get(i).getName());
        intent.putExtra(DevicePairing.EXTRAS_DEVICE_ADDRESS, BluetoothDevices.get(i).getAddress());


        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);
        startActivity(intent);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with " + deviceName);
            BluetoothDevices.get(i).createBond();
        }

    }

    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }

}
