package com.intelliroast.intelliroast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainUI";
    private BluetoothClient client;

    private static final int REQUEST_ENABLE_BT = 1;
    final UUID sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    final String connected = "{\"cmd\":\"Connected\"}";
    final String connectedAck = "{\"cmd\":\"Ack\",\"state\":\"Idle\"}";
    final String startCommand = "{\"cmd\":\"Start\"}";
    final String coolDownCommand = "{\"cmd\":\"Cool\"}";
    final String stopCommand = "{\"cmd\":\"Stop\"}";
    final String ejectCommand = "{\"cmd\":\"Eject\"}";
    final String autoCommand = "{\"cmd\":\"Auto\"}";

    private DrawerLayout mDrawerLayout;

    BluetoothAdapter bluetoothAdapter;

    Button chooseLightRoast;
    Button chooseMediumRoast;
    Button chooseDarkRoast;
    FloatingActionButton startRoast;
    TextView mRoastDetails;
    Spinner mFanSpeed;
    Spinner mPower;
    Button mEndManual;
    Button mStartManual;

    public static Boolean isConnected = false;
    public static Boolean isManual = false;

    public String roastType = "Medium";

    public static String roastState = "";
    public static String timeElapsed = "";
    public static String beanTemp = "";
    public static String setTemp = "";
    public static String elementTemp = "";
    public static String elementPower = "";
    public static String fanSpeed = "";

    public static Integer maxBeanTemp = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isConnected = false;
        isManual = false;

        connectToIntelliRoast();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        chooseLightRoast = findViewById(R.id.doLight);
        chooseMediumRoast = findViewById(R.id.doMedium);
        chooseDarkRoast = findViewById(R.id.doDark);
        startRoast = findViewById(R.id.startRoast);
        mRoastDetails = findViewById(R.id.roast_details);
        mFanSpeed = findViewById(R.id.fan_speed_manual);
        mPower = findViewById(R.id.power_manual);
        mEndManual = findViewById(R.id.endManual);
        mStartManual = findViewById(R.id.startManual);
        Integer[] percentage = new Integer[101];
        for (int i = 0; i < 101; i++) {
            percentage[i] = i;
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, percentage);
//set the spinners adapter to the previously created one.
        mFanSpeed.setAdapter(adapter);
        mFanSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (!isManual) {
                    Log.v(TAG, "Not in Manual Mode");
                    return;
                }
                String percentage = parent.getItemAtPosition(position).toString();
                String output = "Set Fan Speed: " + percentage + "%";
                Log.v(TAG, output);
                String fanSpeedCommand = "{\"cmd\":\"Manual\",\"fan\":\"" +
                        percentage + "\"}";
                if (isConnected) {
                    client.write(fanSpeedCommand.getBytes());
                    showToastShort("Setting Fan Speed to " + percentage + "%");
                } else {
                    Log.v(TAG, "Not connected to IntelliRoast");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });
        mPower.setAdapter(adapter);
        mPower.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (!isManual) {
                    Log.v(TAG, "Not in Manual Mode");
                    return;
                }
                String percentage = parent.getItemAtPosition(position).toString();
                String output = "Set Power: " + percentage + "%";
                Log.v(TAG, output);
                String powerCommand = "{\"cmd\":\"Manual\",\"power\":\"" +
                        percentage + "\"}";
                if (isConnected) {
                    client.write(powerCommand.getBytes());
                    showToastShort("Setting Power to " + percentage + "%");
                } else {
                    Log.v(TAG, "Not connected to IntelliRoast");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub

            }
        });

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.menu_connect:
                                if (isConnected) {
                                    showToast("Already connected");
                                    return true;
                                }
                                connectToIntelliRoast();
                                return true;
                            case R.id.menu_disconnect:
                                disconnectFromIntelliRoast();
                                return true;
                            case R.id.menu_eject:
                                ejectBeans();
                                return true;
                            case R.id.stop_roast:
                                stopRoast();
                                return true;
                            case R.id.menu_em_stop:
                                emergencyStopRoast();
                                return true;
                        }
                        return true;
                    }
                });


        chooseLightRoast.setOnClickListener(this);
        chooseMediumRoast.setOnClickListener(this);
        chooseDarkRoast.setOnClickListener(this);
        startRoast.setOnClickListener(this);
        mEndManual.setOnClickListener(this);
        mStartManual.setOnClickListener(this);
        String roastDetails = "IntelliRoast is currently waiting to roast.";
        mRoastDetails.setText(roastDetails);
    }

    public static class ConnectionHandler extends Handler {
        private MainActivity activity;
        ConnectionHandler(MainActivity displayActivity) {
            activity = displayActivity;
        }
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == BluetoothClient.MessageType.CONNECTION_FAILED) {
                showToast("Bluetooth failed to connect to IntelliRoast");
            }
            else if (msg.what == BluetoothClient.MessageType.CONNECTION_SUCCEEDED) {
                showToast("Connected to IntelliRoast");
            }
            else if (msg.what == BluetoothConnection.MessageType.DISCONNECTED) {
                showToast("Disconnected");
            }
            else if (msg.what == BluetoothConnection.MessageType.READ) {
//                byte[] readBuf = (byte[]) msg.obj;
//                String message = new String(readBuf, 0, msg.arg1);
//                CharBuffer readBuf = (CharBuffer) msg.obj;
//                String message = readBuf.toString();
                String message = (String) msg.obj;
                Log.d(TAG, message);

                // Convert to JSON
                JSONObject messageReceived;
                TextView mRoastDetails = activity.findViewById(R.id.roast_details);
                mRoastDetails.setSingleLine(false);
                String roastDetails;
                try {
                    if (message.startsWith("{\"state\":")) {
                        messageReceived = new JSONObject(message);
                        roastState = messageReceived.get("state").toString();
                        if (roastState.equals("Idle")) {
                            idleState(mRoastDetails);
                            return;
                        }
                        if (roastState.equals("Manual")) {
                            isManual = true;
                        } else {
                            isManual = false;
                        }
                        timeElapsed = messageReceived.get("T").toString();
                        beanTemp = messageReceived.get("BT").toString();
                        setTemp = messageReceived.get("ST").toString();
                        elementTemp = messageReceived.get("ET").toString();
                        elementPower = messageReceived.get("DC").toString();
                        fanSpeed = messageReceived.get("FS").toString();

                        Integer beanTempInt = Integer.parseInt(beanTemp);
                        if (maxBeanTemp < beanTempInt) {
                            maxBeanTemp = beanTempInt;
                        }

                        String secondsString;
                        if (Integer.parseInt(timeElapsed) == 1) {
                            secondsString = " second";
                        } else {
                            secondsString = " seconds";
                        }
                        roastDetails = "IntelliRoast is currently " + roastState + "." +
                                "\nTime Elapsed: " + timeElapsed + secondsString +
                                "\nBean Temp: " + beanTemp + " C" +
                                "\nSet Temp: " + setTemp + " C" +
                                "\nHeating Element Temp: " + elementTemp + " C" +
                                "\nHeating Element Power: " + elementPower + "%" +
                                "\nFan Speed: " + fanSpeed + "%";
                        if (isManual) {
                            roastDetails = "IntelliRoast is currently Roasting in " + roastState + " Mode." +
                                    "\nTime Elapsed: " + timeElapsed + secondsString +
                                    "\nBean Temp: " + beanTemp + " C" +
                                    "\nHeating Element Temp: " + elementTemp + " C" +
                                    "\nHeating Element Power: " + elementPower + "%" +
                                    "\nFan Speed: " + fanSpeed + "%";
                        }
                        mRoastDetails.setText(roastDetails);
                    } else {
                        roastDetails = "IntelliRoast is currently waiting to roast.";
                        mRoastDetails.setText(roastDetails);
                    }
                } catch (org.json.JSONException ex) {

                }


            }
        }

        private void idleState(TextView mRoastDetails) {
            String roastDetails = "IntelliRoast is currently waiting to roast.";
            if (!timeElapsed.equals("")) {
                if (Integer.parseInt(timeElapsed) > 0) {
                    roastDetails = "Last Roast took " + timeElapsed + " seconds." +
                            "\nThe beans reached a maximum temperature of " + maxBeanTemp + " C." +
                            "\nIntelliRoast is currently waiting to roast.";
                }
            }
            mRoastDetails.setText(roastDetails);
        }

        //toast message function
        private void showToast(String msg){
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.doLight:
                // Load Light Roast
                loadRoast("Light");
                break;
            case R.id.doMedium:
                // Load Medium Roast
                loadRoast("Medium");
                break;
            case R.id.doDark:
                // Load Dark Roast
                loadRoast("Dark");
                break;
            case R.id.startRoast:
                // Start roast
                if (isManual) {
                    showToast("You must stop your Manual Roast first!");
                    break;
                }
                client.write(startCommand.getBytes());
                showToast("Starting " + roastType + " Roast!");
                break;
            case R.id.endManual:
                // Go back to Auto
                stopRoast();
                isManual = false;
                break;
            case R.id.startManual:
                // Start a Manual Roast
                String fanSpeed = mFanSpeed.getSelectedItem().toString();
                String power = mPower.getSelectedItem().toString();
                String manualCommand = "{\"cmd\":\"Manual\",\"fan\":\"" +
                        fanSpeed + "\",\"power\":\"" +
                        power + "\"}";
                client.write(manualCommand.getBytes());
                isManual = true;
                showToast("Switched to Manual Mode");
                break;
        }
    }

    public void loadRoast(String type) {
        if (!isConnected) {
            showToast("Not connected to IntelliRoast");
            return;
        }
        String roastProfile;
        switch (type) {
            case "Light":
                roastType = type;
                roastProfile = "{\"cmd\":\"Load\",\"default\":1}";
                break;
            case "Medium":
                roastType = type;
                roastProfile = "{\"cmd\":\"Load\",\"default\":2}";
                break;
            case "Dark":
                roastType = type;
                roastProfile = "{\"cmd\":\"Load\",\"default\":3}";
                break;
            default:
                roastType = "Medium";
                roastProfile = "{\"cmd\":\"Load\",\"default\":2}";
                break;
        }
        client.write(roastProfile.getBytes());
        showToast(roastType + " Roast Loaded");
    }

    // Set up Bluetooth connection
    public void connectToIntelliRoast() {
        if (isConnected) {
            // Already connected :)
            return;
        }
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("Bluetooth is not supported on this device!");
            finish();
        }
        if (!bluetoothAdapter.isEnabled()) {
            isConnected = false;
            showToast("Uh oh! Please turn on your Bluetooth for IntelliRoast to work!");
        } else {
            Handler handler = new ConnectionHandler(this);
            BluetoothDevice bluetoothDevice = null;
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice dev : bondedDevices) {
                if (dev.getName().equals("IntelliRoast")) {
                    bluetoothDevice = dev;
                    break;
                }
            }
            client = new BluetoothClient(bluetoothDevice, handler);
            client.start();
        }
    }

    public void disconnectFromIntelliRoast() {
        if (!isConnected) {
            showToast("Not connected to IntelliRoast");
            return;
        }
        client.cancel();
        showToast("Connection closed");
    }

    public void ejectBeans() {
        if (!isConnected) {
            showToast("Not connected to IntelliRoast");
            return;
        }
        showToast("Ejecting beans");
        client.write(ejectCommand.getBytes());
    }

    public void stopRoast() {
        if (!isConnected) {
            showToast("Not connected to IntelliRoast");
            return;
        }
        showToast("Stopping and cooling down the current roast");
        client.write(coolDownCommand.getBytes());
    }

    public void emergencyStopRoast() {
        if (!isConnected) {
            showToast("Not connected to IntelliRoast");
            return;
        }
        showToast("Turning off Fan and Heat");
        client.write(stopCommand.getBytes());
        isManual = false;
    }

    public void openDevOptions() {
        Intent intent = new Intent(this, DevActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //toast message functions
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
    private void showToastShort(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        client.cancel();
        isManual = false;
        super.onDestroy();
    }

}
