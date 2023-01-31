package com.rashed.arduinobt;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView statusText, onOffText, nameText, interfaceText;
    ImageView statusLight, onOffSwitch, enableBTSwitch, monitoringImage;
    Button sendButton, stopButton;
    AnimationDrawable monitoringAnimation;

    private BluetoothAdapter BA;
    private BluetoothDevice ht05;
    private BluetoothSocket btSocket;

    boolean bluetoothTurnedOn = false;
    String targetedDevice = "20:18:08:34:FC:20";
    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        statusText = (TextView) findViewById(R.id.statusText);
        onOffText = (TextView) findViewById(R.id.onOffText);
        statusLight = (ImageView) findViewById(R.id.statusLight);
        onOffSwitch = (ImageView) findViewById(R.id.onOffSwitch);
        enableBTSwitch = (ImageView) findViewById(R.id.enableBTSwitch);
        sendButton = (Button) findViewById(R.id.sendButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        nameText = (TextView) findViewById(R.id.nameText);
        interfaceText = (TextView) findViewById(R.id.interfaceText);
        monitoringImage = (ImageView) findViewById(R.id.monitoringImage);

        BA = BluetoothAdapter.getDefaultAdapter();
        //System.out.println(btAdapter.getBondedDevices());

        if (BA == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (BA.isEnabled()) {
            bluetoothTurnedOn = true;
            statusText.setText("Bluetooth On");
            onOffText.setText("Connect");
            enableBTSwitch.setImageResource(R.drawable.bt_on);
            statusLight.setImageResource(R.drawable.status_active);
        }

        enableBTSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothTurnedOn) {
                    checkForBTPermission();
                    BA.disable();
                    bluetoothTurnedOn = false;
                    statusText.setText("Bluetooth Off");
                    onOffText.setText("Off");
                    enableBTSwitch.setImageResource(R.drawable.bt_off);
                    statusLight.setImageResource(R.drawable.status_inactive);
                    Toast.makeText(MainActivity.this, "Bluetooth Turned Off", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intentOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intentOn,0);
                    bluetoothTurnedOn = true;
                    statusText.setText("Bluetooth On");
                    onOffText.setText("Connect");
                    enableBTSwitch.setImageResource(R.drawable.bt_on);
                    statusLight.setImageResource(R.drawable.status_active);
                    Toast.makeText(MainActivity.this, "Bluetooth Turned On", Toast.LENGTH_SHORT).show();
                }
            }
        });

        onOffSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!bluetoothTurnedOn) {
                    onOffText.setText("Off");
                    Toast.makeText(MainActivity.this, "Bluetooth Off", Toast.LENGTH_SHORT).show();
                } else {
                    onOffText.setText("Monitoring");
                    ht05 = BA.getRemoteDevice(targetedDevice);
                    checkForBTPermission();
                    nameText.setText(ht05.getName());
                    interfaceText.setText(targetedDevice);
                    onOffSwitch.setImageResource(R.drawable.main_button_active);

                    monitoringImage.setBackgroundResource(R.drawable.monitoring_sequence);
                    monitoringAnimation = (AnimationDrawable) monitoringImage.getBackground() ;
                    monitoringAnimation.start();
                }
            }
        });

//        conButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                int numberOfTries = 0;
//                do {
//                    try {
//                        checkForBTPermission();
//                        btSocket = ht05.createRfcommSocketToServiceRecord(mUUID);
//                        System.out.println(btSocket);
//
//                        btSocket.connect();
//                        //System.out.println(btSocket.isConnected());
//                        infoText.setText(ht05.getName() + " Connected=" + btSocket.isConnected());
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    numberOfTries++;
//                } while (!btSocket.isConnected() && numberOfTries<3);
//            }
//        });

        sendButton.setOnClickListener(new View.OnClickListener() {
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



        stopButton.setOnClickListener(new View.OnClickListener() {
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