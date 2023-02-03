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
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView activityLog, statusText, onOffText, nameText, interfaceText;
    ImageView statusLight, onOffSwitch, enableBTSwitch, monitoringImage;
    Button connectButton, sendTestButton, stopButton;
    Spinner deviceSinner;
    AnimationDrawable monitoringAnimation;

    private BluetoothAdapter btAdapter;
    private Set<BluetoothDevice> btDevices;
    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private DataCommunication dataComm;
    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    static public final int BT_CON_STATUS_NOT_CONNECTED = 0;
    static public final int BT_CON_STATUS_CONNECTING = 1;
    static public final int BT_CON_STATUS_CONNECTED = 2;
    static public final int BT_CON_STATUS_FAILED = 3;
    static public final int BT_CON_STATUS_CONNECTION_LOST = 4;

    static public final int BT_STATE_LISTENING = 1;
    static public final int BT_STATE_CONNECTING = 2;
    static public final int BT_STATE_CONNECTED = 3;
    static public final int BT_STATE_CONNECTION_FAILED = 4;
    static public final int BT_STATE_MSG_RECEIVED = 5;

    static public int bluetooth_status = BT_CON_STATUS_NOT_CONNECTED;
    boolean bluetooth_TurnedOn = false;
    boolean bluetooth_Connected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        activityLog = (TextView) findViewById(R.id.activityLog);
        statusText = (TextView) findViewById(R.id.statusText);
        onOffText = (TextView) findViewById(R.id.onOffText);
        nameText = (TextView) findViewById(R.id.nameText);
        interfaceText = (TextView) findViewById(R.id.interfaceText);
        deviceSinner = (Spinner) findViewById(R.id.deviceSinner);
        statusLight = (ImageView) findViewById(R.id.statusLight);
        onOffSwitch = (ImageView) findViewById(R.id.onOffSwitch);
        enableBTSwitch = (ImageView) findViewById(R.id.enableBTSwitch);
        monitoringImage = (ImageView) findViewById(R.id.monitoringImage);
        connectButton = (Button) findViewById(R.id.connectButton);
        sendTestButton = (Button) findViewById(R.id.sendTestButton);
        stopButton = (Button) findViewById(R.id.stopButton);

        activityLog.setMovementMethod(new ScrollingMovementMethod());

        enableBTSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetooth_TurnedOn) {
                    checkForBTPermission();
                    btAdapter.disable();
                    bluetooth_TurnedOn = false;
                    statusText.setText("Bluetooth Off");
                    onOffText.setText("Off");
                    enableBTSwitch.setImageResource(R.drawable.bt_off);
                    statusLight.setImageResource(R.drawable.status_inactive);
                    Toast.makeText(MainActivity.this, "Bluetooth Turned Off", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intentOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intentOn,0);
                    bluetooth_TurnedOn = true;
                    statusText.setText("Bluetooth On");
                    onOffText.setText("Connect");
                    enableBTSwitch.setImageResource(R.drawable.bt_on);
                    statusLight.setImageResource(R.drawable.status_active);
                    Toast.makeText(MainActivity.this, "Bluetooth Turned On", Toast.LENGTH_SHORT).show();
                }
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bluetooth_Connected == false) {
                    if (deviceSinner.getSelectedItemPosition() == 0) {
                        Toast.makeText(MainActivity.this, "No Device Selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String selectedDevice = deviceSinner.getSelectedItem().toString();
                    for (BluetoothDevice device : btDevices) {
                        if (selectedDevice.equals(device.getName())) {
                            btDevice = device;
                            activityLog.append("\n► Selected device \"" + btDevice.getName() + " : " + btDevice.getAddress() + "\"");

                            ConnectBluetooth cbt = new ConnectBluetooth(btDevice);
                            activityLog.append("\n► Connecting to device... ");
                            cbt.start();
                        }
                    }
                } else {
                    activityLog.append("\n► Disconnecting from device... ");
                    if(btSocket != null && btSocket.isConnected()) {
                        try {
                            btSocket.close();
                            if (!btSocket.isConnected()) { activityLog.append(" SUCCESS "); }
                        } catch (IOException e) {
                            e.printStackTrace();
                            activityLog.append(" FAILED ");
                        }
                    }
                    connectButton.setText("CONNECT");
                    bluetooth_Connected = false;
                }
            }
        });

        sendTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(btSocket == null) {
                    activityLog.append("\n► No Device Connected");
                    return;
                }
                sendMessage("0");
                activityLog.append("\n► SND : Test Data Sent");
            }
        });







//        onOffSwitch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (!bluetoothTurnedOn) {
//                    onOffText.setText("Off");
//                    Toast.makeText(MainActivity.this, "Bluetooth Off", Toast.LENGTH_SHORT).show();
//                } else {
//                    onOffText.setText("Monitoring");
//                    btDevices = btAdapter.getRemoteDevice(targetedDevice);
//                    checkForBTPermission();
//                    nameText.setText(btDevices.getName());
//                    interfaceText.setText(targetedDevice);
//                    onOffSwitch.setImageResource(R.drawable.main_button_active);
//
//                    monitoringImage.setBackgroundResource(R.drawable.monitoring_sequence);
//                    monitoringAnimation = (AnimationDrawable) monitoringImage.getBackground() ;
//                    monitoringAnimation.start();
//                }
//            }
//        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        getPairedDevices();
        populateSpinner();
    }

    void getPairedDevices() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            //finish();
            return;
        } else if (!btAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
            return;
        }
        btDevices = btAdapter.getBondedDevices();
        activityLog.append("► " + btDevices.size() + " Devices Found");
    }

    void populateSpinner() {
        ArrayList<String> allDevices = new ArrayList<>();
        allDevices.add("Select Device");
        for (BluetoothDevice btDevice : btDevices) {
            allDevices.add(btDevice.getName());
        }
        final ArrayAdapter<String> allDevicesAdapter = new ArrayAdapter<String>(this,androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,allDevices);
        allDevicesAdapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        deviceSinner.setAdapter(allDevicesAdapter);
    }

    private void checkForBTPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "No Permission", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public class ConnectBluetooth extends Thread {
        private BluetoothDevice device;

        public ConnectBluetooth (BluetoothDevice btDevice) {
            device = btDevice;
            try {
                btSocket = device.createRfcommSocketToServiceRecord(mUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            int numberOfTries = 0;
            do {
                try {
                    btSocket.connect();
                    if (btSocket.isConnected()) {
                        Message msg = Message.obtain();
                        msg.what = BT_STATE_CONNECTED;
                        handler.sendMessage(msg);
                        break;
                    }
                } catch (IOException e) {
                    Message msg = Message.obtain();
                    msg.what = BT_STATE_CONNECTION_FAILED;
                    handler.sendMessage(msg);
                    e.printStackTrace();
                }
                numberOfTries++;
            } while (!btSocket.isConnected() && numberOfTries < 3);
        }
    }

    public class DataCommunication extends Thread {
        private final BluetoothSocket btSkt;
        private OutputStream outputStream;
        private InputStream inputStream;

        public DataCommunication (BluetoothSocket socket) {
            System.out.println("Data Comm started");
            btSkt = socket;

            try {
                inputStream = btSkt.getInputStream();
                outputStream = btSkt.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (btSkt.isConnected()) {
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(BT_STATE_MSG_RECEIVED, bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    bluetooth_status = BT_CON_STATUS_CONNECTION_LOST;
                    activityLog.append("\n► ERR : Bluetooth connection lost");
                    try {
                        if (btSkt != null && btSkt.isConnected()) btSkt.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case BT_STATE_LISTENING:
                    //listening animation
                    System.out.println("listening");
                    break;
                case BT_STATE_CONNECTING:
                    bluetooth_status = BT_CON_STATUS_CONNECTING;
                    connectButton.setText("CONNECTING");
                    break;
                case BT_STATE_CONNECTED:
                    bluetooth_status = BT_CON_STATUS_CONNECTED;
                    activityLog.append(" SUCCESS ");
                    connectButton.setText("DISCONNECT");

                    dataComm = new DataCommunication(btSocket);
                    dataComm.start();

                    bluetooth_Connected = true;
                    break;
                case BT_STATE_CONNECTION_FAILED:
                    bluetooth_status = BT_CON_STATUS_FAILED;
                    activityLog.append(" FAILED.. ");
                    bluetooth_Connected = false;
                    break;
                case BT_STATE_MSG_RECEIVED:
                    byte[] inputBuffer = (byte[]) message.obj;
                    String recievedMsg = new String(inputBuffer,0,message.arg1);

                    if (recievedMsg.equals('0')) {
                        activityLog.append("\n► ALERT: Triggered!");
                    } else if (recievedMsg.equals('R')) {
                        activityLog.append("\n► ACK: Test data received!");
                    } else {
                        activityLog.append("\n► MSG[" + recievedMsg.length() + "] : " + recievedMsg);
                    }

                    break;
            }
            return true;
        }
    });

    public void sendMessage(String data) {
        if (btSocket != null && btSocket.isConnected()) {
            try {
                dataComm.write(data.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}