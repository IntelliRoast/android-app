package com.intelliroast.intelliroast;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BluetoothService extends Service {

    //TODO: Add a "WAITING_FOR_ACK" State and then implement a way to look and wait for the acks

    // Binder given to clients
    private final IBinder binder = new BluetoothBinder();
    // Debugging
    private static final String TAG = "BluetoothService";

    // Name for the SDP record when creating server socket
    private static final String NAME_INSECURE = "BluetoothIntelliRoast";

    private static final UUID serialUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    //private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    public IntelliRoastState machineState;
    private Handler mHandler = null;
    private int mState;
    private int mNewState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2; // now connected to a remote device

    final String connected = "{\"cmd\":\"Connected\"}";
    final String connectedAck = "{\"cmd\":\"Ack\",\"state\":\"Idle\"}";
    final String startCommand = "{\"cmd\":\"Start\"}";
    final String coolDownCommand = "{\"cmd\":\"Cool\"}";
    final String stopCommand = "{\"cmd\":\"Stop\"}";
    final String ejectCommand = "{\"cmd\":\"Eject\"}";
    final String autoCommand = "{\"cmd\":\"Auto\"}";
    final String roastDetailsCommand = "{\"cmd\":\"RoastDetails\"}";


    public BluetoothService() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
    }

    public class BluetoothBinder extends Binder {
        BluetoothService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothService.this;
        }
    }

    @Override
    public void onCreate() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy(){
        stop(); //stops all active threads and cleans things up
    }

    public synchronized int getState() {
        return mState;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    private synchronized void notifyStateChange() {
        mState = getState();
        Log.d(TAG, "notifyStateChange() " + mNewState + " -> " + mState);
        mNewState = mState;
        Message message = mHandler.obtainMessage(constants.MESSAGE_STATE_CHANGE);

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(constants.MESSAGE_STATE_CHANGE).sendToTarget();
    }

    /**
     * Start the chat service. Specifically start ConnectThread to begin a
     * session in Scanning for IntelliRoast. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
        // Notify
        notifyStateChange();

    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;

    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;

    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(serialUUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket: create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
            notifyStateChange();
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with an IntelliRoast machine.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private BufferedReader reader;
        private JSONObject messageReceived;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread: ");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            reader = new BufferedReader(new InputStreamReader(tmpIn, StandardCharsets.US_ASCII));
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
            notifyStateChange();
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
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
                        mHandler.obtainMessage(constants.MESSAGE_MACHINE_STATE).sendToTarget();

                    } else {
                        // Send the obtained bytes to the UI Activity
                        mHandler.obtainMessage(constants.MESSAGE_READ, message).sendToTarget();
                    }
                    Thread.sleep(1000);

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                } catch (InterruptedException e) {
                    Log.e(TAG,"connectedThread: InterruptedException");
                    connectionFailed();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    public void ejectBeans() {
        if (mState == STATE_CONNECTED) {
            mConnectedThread.write(ejectCommand.getBytes());
        }
    }

    public void startRoast() {
        if (mState == STATE_CONNECTED) {
            mConnectedThread.write(startCommand.getBytes());
        }
    }

    public void stopRoast() {
        if (mState == STATE_CONNECTED) {
            mConnectedThread.write(coolDownCommand.getBytes());
        }
    }

    public void startManualMode(String manualCommand) {
        if (mState == STATE_CONNECTED) {
            mConnectedThread.write(manualCommand.getBytes());
        }
    }

    public void loadRoast(String type) {
        //TODO: HANDLE CUSTOM ROASTS (soon to be only type of roasts)
        if (mState == STATE_CONNECTED) {
            String roastProfile;
            switch (type) {
                case "Light":
                    roastProfile = "{\"cmd\":\"Load\",\"default\":1}";
                    break;
                case "Medium":
                    roastProfile = "{\"cmd\":\"Load\",\"default\":2}";
                    break;
                case "Dark":
                    roastProfile = "{\"cmd\":\"Load\",\"default\":3}";
                    break;
                default:
                    roastProfile = "{\"cmd\":\"Load\",\"default\":2}";
                    break;
            }
            mConnectedThread.write(roastProfile.getBytes());
        }
    }

    public void setFanSpeed(String fanspeed) {
        if (mState == STATE_CONNECTED) {
            String fanSpeedCommand = "{\"cmd\":\"Manual\",\"fan\":\"" +
                    fanspeed + "\"}";
            mConnectedThread.write(fanSpeedCommand.getBytes());
        }
    }

    public void setPower(String fanspeed) {
        if (mState == STATE_CONNECTED) {
            String powerCommand = "{\"cmd\":\"Manual\",\"power\":\"" +
                    fanspeed + "\"}";
            mConnectedThread.write(powerCommand.getBytes());
        }
    }

    public void emergencyStopRoast() {
        if (mState == STATE_CONNECTED) {
            mConnectedThread.write(stopCommand.getBytes());
        }
    }
}
