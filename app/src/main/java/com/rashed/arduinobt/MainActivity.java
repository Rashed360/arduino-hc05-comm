package com.rashed.arduinobt;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView infoText;
    Button initButton, conButton, sndButton, disButton;
    CheckBox enableBT;

    private BluetoothAdapter BA;
    private BluetoothDevice ht05;
    private BluetoothSocket btSocket;

    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        infoText = (TextView) findViewById(R.id.infoText);
        initButton = (Button) findViewById(R.id.initButton);
        conButton = (Button) findViewById(R.id.conButton);
        sndButton = (Button) findViewById(R.id.sndButton);
        disButton = (Button) findViewById(R.id.disButton);
        enableBT = (CheckBox) findViewById(R.id.enableBT);

        BA = BluetoothAdapter.getDefaultAdapter();
        //System.out.println(btAdapter.getBondedDevices());

        if (BA == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (BA.isEnabled()) {
            enableBT.setChecked(true);
        }

        enableBT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (!isChecked) {
                    checkForBTPermission();
                    BA.disable();
                    Toast.makeText(MainActivity.this, "Turned Off", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intentOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intentOn,0);
                    Toast.makeText(MainActivity.this, "Turned On", Toast.LENGTH_SHORT).show();
                }
            }
        });

        initButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ht05 = BA.getRemoteDevice("20:18:08:34:FC:20");
                //System.out.println(ht05.getName());

                checkForBTPermission();
                infoText.setText(ht05.getName());
            }
        });

        conButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int numberOfTries = 0;
                do {
                    try {
                        checkForBTPermission();
                        btSocket = ht05.createRfcommSocketToServiceRecord(mUUID);
                        System.out.println(btSocket);

                        btSocket.connect();
                        //System.out.println(btSocket.isConnected());
                        infoText.setText(ht05.getName() + " Connected=" + btSocket.isConnected());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    numberOfTries++;
                } while (!btSocket.isConnected() && numberOfTries<3);
            }
        });

        sndButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    OutputStream outputStream = btSocket.getOutputStream();
                    outputStream.write(48);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    InputStream inputStream = btSocket.getInputStream();
                    inputStream.skip(inputStream.available());

                    for(int i = 0; i < 26; i++) {
                        byte b = (byte) inputStream.read();
                        System.out.println((char) b);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });



        disButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    btSocket.close();
                    System.out.println(btSocket.isConnected());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void checkForBTPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No Permission", Toast.LENGTH_SHORT).show();
            return;
        }
    }
}