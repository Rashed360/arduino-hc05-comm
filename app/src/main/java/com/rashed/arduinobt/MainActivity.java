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
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    TextView activityLog, statusText, onOffText, nameText, interfaceText;
    ImageView statusLight, onOffSwitch, enableBTSwitch, monitoringImage, clearView;
    Button connectButton, sendTestButton, stopButton;
    Spinner deviceSpinner;
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
        deviceSpinner = (Spinner) findViewById(R.id.deviceSpinner);
        statusLight = (ImageView) findViewById(R.id.statusLight);
        onOffSwitch = (ImageView) findViewById(R.id.onOffSwitch);
        enableBTSwitch = (ImageView) findViewById(R.id.enableBTSwitch);
        monitoringImage = (ImageView) findViewById(R.id.monitoringImage);
        clearView = (ImageView) findViewById(R.id.clearView);
        connectButton = (Button) findViewById(R.id.connectButton);
        sendTestButton = (Button) findViewById(R.id.sendTestButton);
        stopButton = (Button) findViewById(R.id.stopButton);

        activityLog.setMovementMethod(new ScrollingMovementMethod());

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth Not Supported", Toast.LENGTH_SHORT).show();
            activityLog.append("\n► Your Device doesn't Support Bluetooth");
        } else if (!btAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth Not Enabled", Toast.LENGTH_SHORT).show();
            onOffVisual(false);
        } else {
            btDevices = btAdapter.getBondedDevices();
            activityLog.append("► " + btDevices.size() + " Devices Found");
            onOffVisual(true);
            populateSpinner();
        }

        enableBTSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetooth_TurnedOn) {
                    checkForBTPermission();
                    btAdapter.disable();
                    Toast.makeText(MainActivity.this, "Bluetooth Turned Off", Toast.LENGTH_SHORT).show();
                    onOffVisual(false);
                    monitoringVisual(false);
                } else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 0);
                }
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!btAdapter.isEnabled()) {
                    Toast.makeText(MainActivity.this, "Bluetooth Off", Toast.LENGTH_SHORT).show();
                    return;
                } else if(bluetooth_Connected == false) {
                    if (deviceSpinner.getSelectedItemPosition() == 0) {
                        Toast.makeText(MainActivity.this, "No Device Selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String selectedDevice = deviceSpinner.getSelectedItem().toString();
                    for (BluetoothDevice device : btDevices) {
                        if (selectedDevice.equals(device.getName())) {
                            btDevice = device;
                            nameText.setText(btDevice.getName());
                            interfaceText.setText(btDevice.getAddress());
                            activityLog.append("\n► Selected Device \"" + btDevice.getName() + " : " + btDevice.getAddress() + "\"");

                            ConnectBluetooth cbt = new ConnectBluetooth(btDevice);
                            activityLog.append("\n► Connecting to Device... ");
                            cbt.start();
                        }
                    }
                } else {
                    activityLog.append("\n► Disconnecting from Device... ");
                    if(btSocket != null && btSocket.isConnected()) {
                        try {
                            btSocket.close();
                            monitoringVisual(false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    connectButton.setText("CONNECT");
                    bluetooth_Connected = false;
                }
            }
        });

        clearView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityLog.setText("");
            }
        });

        sendTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!bluetooth_TurnedOn) {
                    activityLog.append("\n► Bluetooth Off or Not Supported");
                } else if (!bluetooth_Connected) {
                    activityLog.append("\n► No Device Connected");
                } else {
                    sendMessage("0");
                    activityLog.append("\n► Test Data Sent to " + btDevice.getName());
                }
            }
        });
    }

    void populateSpinner() {
        ArrayList<String> allDevices = new ArrayList<>();
        allDevices.add("Select Device");
        for (BluetoothDevice btDevice : btDevices) {
            allDevices.add(btDevice.getName());
        }
        final ArrayAdapter<String> allDevicesAdapter = new ArrayAdapter<String>(MainActivity.this,androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,allDevices);
        allDevicesAdapter.setDropDownViewResource(androidx.appcompat.R.layout.support_simple_spinner_dropdown_item);
        deviceSpinner.setAdapter(allDevicesAdapter);
    }

    void onOffVisual(boolean state) {
        if (state == true) {
            bluetooth_TurnedOn = true;
            statusLight.setImageResource(R.drawable.status_active);
            statusText.setText("Bluetooth On");
            enableBTSwitch.setImageResource(R.drawable.bt_on);
            onOffSwitch.setImageResource(R.drawable.main_button_available);
            onOffText.setText("Connect");
        } else {
            bluetooth_TurnedOn = false;
            statusLight.setImageResource(R.drawable.status_inactive);
            statusText.setText("Bluetooth Off");
            enableBTSwitch.setImageResource(R.drawable.bt_off);
            onOffSwitch.setImageResource(R.drawable.main_button_inactive);
            onOffText.setText("Off");
        }
    }

    void monitoringVisual(boolean state) {
        monitoringImage.setBackgroundResource(R.drawable.monitoring_sequence);
        monitoringAnimation = (AnimationDrawable) monitoringImage.getBackground();
        if (state == true) {
            onOffText.setText(R.string.bt_state_monitoring);
            onOffSwitch.setImageResource(R.drawable.main_button_active);
            monitoringAnimation.start();
            return;
        }
        if (bluetooth_TurnedOn == true) {
            onOffText.setText(R.string.bt_state_connect);
            onOffSwitch.setImageResource(R.drawable.main_button_available);
        } else {
            onOffText.setText(R.string.bt_state_off);
            onOffSwitch.setImageResource(R.drawable.main_button_inactive);
        }
        monitoringImage.setBackgroundResource(0);
        monitoringAnimation.stop();
    }

    void triggeredVisual() {
        monitoringImage.setBackgroundResource(R.drawable.triggered_sequence);
        monitoringAnimation = (AnimationDrawable) monitoringImage.getBackground();
        onOffText.setText(R.string.bt_state_triggered);
        onOffSwitch.setImageResource(R.drawable.main_button_error);
        monitoringAnimation.start();
        setTimeout(() -> monitoringVisual(true), 1000);
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
            Message msg1 = Message.obtain();
            msg1.what = BT_STATE_CONNECTING;
            handler.sendMessage(msg1);
            int numberOfTries = 0;
            do {
                try {
                    btSocket.connect();
                    if (btSocket.isConnected()) {
                        Message msg2 = Message.obtain();
                        msg2.what = BT_STATE_CONNECTED;
                        handler.sendMessage(msg2);
                        break;
                    }
                } catch (IOException e) {
                    Message msg3 = Message.obtain();
                    msg3.what = BT_STATE_CONNECTION_FAILED;
                    handler.sendMessage(msg3);
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
            activityLog.append("\n► Waiting for Trigger...");
            btSkt = socket;
            Message msg = Message.obtain();
            msg.what = BT_STATE_LISTENING;
            handler.sendMessage(msg);
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
                    connectButton.setText("CONNECT");
                    activityLog.append("\n► Bluetooth Connection Lost");
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
                    monitoringVisual(true);
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
                    if (recievedMsg.contains("lert")) {
                        activityLog.append("\nALERT: Triggered!");
                        triggeredVisual();
                    } else if (recievedMsg.contains("eceived")) {
                        activityLog.append("\nACK: Test data received!");
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

    public static void setTimeout(Runnable runnable, int delay){
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                runnable.run();
            }
            catch (Exception e){
                System.err.println(e);
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==0) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "Bluetooth Turned On", Toast.LENGTH_SHORT).show();
                onOffVisual(true);
                btDevices = btAdapter.getBondedDevices();
                activityLog.append("\n► " + btDevices.size() + " Devices Found");
                populateSpinner();
            } else {
                Toast.makeText(MainActivity.this, "Bluetooth request denied", Toast.LENGTH_SHORT).show();
                onOffVisual(false);
            }
        }
    }
}