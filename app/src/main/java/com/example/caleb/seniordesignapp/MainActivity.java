package com.example.caleb.seniordesignapp;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    TextView Message_TextView;
    Button Bluetooth_Btn;
    Button Connect_Btn;
    BluetoothAdapter myBluetoothAdapter;
    private static final String TAG = MainActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Message_TextView = (TextView)findViewById(R.id.TextBox);
        Bluetooth_Btn = (Button)findViewById(R.id.BluetoothBtn);
        Connect_Btn = (Button)findViewById(R.id.connect_Btn);
        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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


        //Green #78AB46
    }


    public void TurnOn(){
        Bluetooth_Btn.setText("Bluetooth : ON");
        Bluetooth_Btn.setBackgroundColor(Color.GREEN);

        if(myBluetoothAdapter == null){
            Log.d(TAG, "Phone does not have bluetooth capabilities");
        }
        Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(enableBTIntent);

        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        //registerReceiver(mBroadcastReceiver1, BTIntent);

    }

    public void TurnOff(){
        Bluetooth_Btn.setText("Bluetooth : OFF");
        Bluetooth_Btn.setBackgroundColor(Color.RED);
    }
}
