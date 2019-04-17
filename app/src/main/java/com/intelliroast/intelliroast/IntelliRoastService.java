package com.intelliroast.intelliroast;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class IntelliRoastService extends Service {
    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    private boolean isConnected = false;
    IntelliRoastState machineState;
    private BluetoothClient client;
    private static final String TAG = "IntelliRoastService";

    final String connected = "{\"cmd\":\"Connected\"}";
    final String connectedAck = "{\"cmd\":\"Ack\",\"state\":\"Idle\"}";
    final String startCommand = "{\"cmd\":\"Start\"}";
    final String coolDownCommand = "{\"cmd\":\"Cool\"}";
    final String stopCommand = "{\"cmd\":\"Stop\"}";
    final String ejectCommand = "{\"cmd\":\"Eject\"}";
    final String autoCommand = "{\"cmd\":\"Auto\"}";
    final String roastDetailsCommand = "{\"cmd\":\"RoastDetails\"}";

    BluetoothAdapter bluetoothAdapter;
    private static final UUID serialUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    public class LocalBinder extends Binder {
        IntelliRoastService getService() {
            // Return this instance of LocalService so clients can call public methods
            return IntelliRoastService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        connectToIntelliRoast();

    }

    @Override
    public void onDestroy(){
        disconnectFromIntelliRoast();
    }

    class MessageType {
        static final int DISCONNECTED = 0;
        static final int CONNECTED = 1;
        static final int CONNECTION_FAILED = 2;
        static final int NO_BLUETOOTH_ADAPTER = 3;
        static final int BLUETOOTH_OFF = 4;
        static final int NOT_PAIRED = 5;
        static final int MESSAGE_SENT = 6;
        static final int MESSAGE_FAILED = 7;
        static final int MESSAGE_NOT_ACKED = 8;
    }
    public class BluetoothClient extends Thread {
        private BluetoothSocket socket;
        private BufferedReader reader;
        private OutputStream outputStream;
        private ReentrantLock readLock = new ReentrantLock();
        private JSONObject messageReceived;



        BluetoothClient(BluetoothDevice device) {
            //open socket
            try {
                socket = device.createRfcommSocketToServiceRecord(serialUUID);    socket.connect();
            } catch (IOException connectException) {
                try {
                    socket.close();
                    Log.e(TAG, "Could not open bluetooth client socket");
                    isConnected = false;
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the bluetooth client socket", closeException);
                }
            }
            //setup input and output streams for socket
            InputStream tmpIn;
            BufferedReader tmpReader = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpReader = new BufferedReader(new InputStreamReader(tmpIn, StandardCharsets.US_ASCII));
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }
            reader = tmpReader;
            outputStream = tmpOut;
        }

        public void run() {


            while (true) {
                try {
                    Thread.sleep(1000);
                    readLock.lock();
                    write(roastDetailsCommand.getBytes());
                    //get latest data from machine
                    String message = reader.readLine();
                    if (message.startsWith("{\"state\":")) {
                        try {
                            messageReceived = new JSONObject(message);
                            machineState.roastState = messageReceived.get("state").toString();
                            machineState.timeElapsed = messageReceived.get("T").toString();
                            machineState.beanTemp = messageReceived.get("BT").toString();
                            machineState.setTemp = messageReceived.get("ST").toString();
                            machineState.inputTemp = messageReceived.get("IT").toString();
                            machineState.exhaustTemp = messageReceived.get("ET").toString();
                            machineState.elementPower = messageReceived.get("DC").toString();
                            machineState.fanSpeed = messageReceived.get("FS").toString();

                            Integer beanTempInt = Integer.parseInt(machineState.beanTemp);
                            if (machineState.maxBeanTemp < beanTempInt) {
                                machineState.maxBeanTemp = beanTempInt;
                            }
                            Integer roastTimeInt = Integer.parseInt(machineState.timeElapsed);
                            if (machineState.roastTime < roastTimeInt) {
                                machineState.roastTime = roastTimeInt;
                            }
                        } catch (JSONException e) {
                            Log.e(TAG,"Error creating JSONObject", e);
                        }

                    }
                    //handle message.
                } catch (IOException e) {
                    Log.e(TAG, "Input stream was disconnected", e);
                    break;
                } catch (InterruptedException e) {
                    Log.e(TAG, "Thread.sleep messed things up");
                    break;
                }
                finally {
                    readLock.unlock();
                }
            }
        }

       int write(byte[] bytes){
            try {
                readLock.lock();
                outputStream.write(bytes);
                String message = reader.readLine();
                if (message.contains("\"Ack\"")) {
                    return MessageType.MESSAGE_SENT;
                } else {
                    return MessageType.MESSAGE_NOT_ACKED;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
                return MessageType.MESSAGE_FAILED;
            }
            finally {
                readLock.unlock();
            }
        }



        void cancel(){
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    public int connectToIntelliRoast() {
        if(!isConnected) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                return MessageType.NO_BLUETOOTH_ADAPTER;
            }
            if (!bluetoothAdapter.isEnabled()) {
                isConnected = false;
                return MessageType.BLUETOOTH_OFF;
            } else {
                BluetoothDevice bluetoothDevice = null;
                Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice dev : bondedDevices) {
                    if (dev.getName().contains("IntelliRoast")) {
                        bluetoothDevice = dev;
                        break;
                    }
                }
                if (bluetoothDevice == null) {
                    isConnected = false;
                    return MessageType.NOT_PAIRED;
                } else {
                    client = new BluetoothClient(bluetoothDevice);
                    client.start();
                    client.write(connected.getBytes());
                    isConnected = true;
                    return MessageType.CONNECTED;
                }
            }
        } else {
            return MessageType.CONNECTED;
        }
    }

    public int disconnectFromIntelliRoast() {
        client.cancel();
        isConnected = false;
        return MessageType.DISCONNECTED;
    }

    public int ejectBeans() {
        if (!isConnected) {
            return MessageType.DISCONNECTED;
        }
        return client.write(ejectCommand.getBytes());
    }

    public int stopRoast() {
        if (!isConnected) {
            return MessageType.DISCONNECTED;
        }
        return client.write(coolDownCommand.getBytes());
    }

    public int emergencyStopRoast() {
        if (!isConnected) {
            return MessageType.DISCONNECTED;
        }
        return client.write(stopCommand.getBytes());
    }
}
