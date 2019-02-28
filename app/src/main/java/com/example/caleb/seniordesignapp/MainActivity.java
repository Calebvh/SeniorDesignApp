package com.example.caleb.seniordesignapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static com.example.caleb.seniordesignapp.R.id.connect_Btn;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    TextView Message_TextView;
    Button Bluetooth_Btn;
    Button Find_Btn;
    BluetoothAdapter myBluetoothAdapter;
    BluetoothDevice myBluetoothDevice;

    private static final String TAG = MainActivity.class.getName();

    DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;

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
                BluetoothDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, BluetoothDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Message_TextView = (TextView)findViewById(R.id.TextBox);
        Bluetooth_Btn = (Button)findViewById(R.id.BluetoothBtn);
        Find_Btn = (Button) findViewById(R.id.Find_Btn);

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        myBluetoothAdapter = bluetoothManager.getAdapter();

        //myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        lvNewDevices = (ListView) findViewById(R.id._listView);


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

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

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
