package com.intelliroast.intelliroast;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.Set;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainUI";


    private DrawerLayout mDrawerLayout;

    Button chooseLightRoast;
    Button chooseMediumRoast;
    Button chooseDarkRoast;
    FloatingActionButton startRoast;
    TextView mRoastDetails;
    Spinner mFanSpeed;
    Spinner mPower;
    Button mEndManual;
    Button mStartManual;
    ImageView lightRoastImageView;
    ImageView medRoastImageView;
    ImageView darkRoastImageView;

    public static Boolean isConnected = false;
    public static Boolean isManual = false;

    public String roastType = "Medium";

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 3;


    BluetoothService mService;
    BluetoothAdapter mAdapter;
    BluetoothDevice mDevice;
    Handler mHandler = new ConnectionHandler(MainActivity.this);
    IntelliRoastState mMachineState;
    boolean mBound = false;

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothService.BluetoothBinder binder = (BluetoothService.BluetoothBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.setHandler(mHandler);
            connectToIntelliRoast();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isConnected = false;
        isManual = false;

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


        lightRoastImageView = findViewById(R.id.imageView4);
        medRoastImageView = findViewById(R.id.imageView5);
        darkRoastImageView = findViewById(R.id.imageView6);
        Picasso.get().load(R.drawable.light_roast).into(lightRoastImageView);
        Picasso.get().load(R.drawable.medium_roast).into(medRoastImageView);
        Picasso.get().load(R.drawable.dark_roast).into(darkRoastImageView);


        Integer[] percentage = new Integer[101];
        for (int i = 0; i < 101; i++) {
            percentage[i] = i;
        }
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, percentage);
//set the spinners adapter to the previously created one.

        //Manual Mode Fan Speed
        mFanSpeed.setAdapter(adapter);
        mFanSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (mService != null) {
                    if (mService.getState() == mService.STATE_CONNECTED) {
                        if (mService.machineState.roastState.contains("Manual")) {
                            Log.v(TAG, "Not in Manual Mode");
                            return;
                        }
                        String percentage = parent.getItemAtPosition(position).toString();
                        String output = "Set Fan Speed: " + percentage + "%";
                        Log.v(TAG, output);
                        mService.setFanSpeed(percentage);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //Manual Mode Power
        mPower.setAdapter(adapter);
        mPower.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (mService != null) {
                    if (mService.getState() == mService.STATE_CONNECTED) {
                        if (mService.machineState.roastState.contains("Manual")) {
                            Log.v(TAG, "Not in Manual Mode");
                            return;
                        }
                        String percentage = parent.getItemAtPosition(position).toString();
                        String output = "Set Power: " + percentage + "%";
                        Log.v(TAG, output);
                        mService.setPower(percentage);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        //Drawer Options
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.menu_connect:
                                if (mService.getState() == mService.STATE_CONNECTED) {
                                    showToast("Already connected");
                                    return true;
                                }
                                connectToIntelliRoast();
                                return true;
                            case R.id.menu_disconnect:
                                mService.stop();
                                return true;
                            case R.id.stop_roast:
                                mService.stopRoast();
                                return true;
                            case R.id.menu_em_stop:
                                mService.emergencyStopRoast();
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


    /**
     * Sets up and connects IntelliRoast
     *
     */
    public void connectToIntelliRoast() {
        mDevice = null;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            //tell the user bluetooth is not supported by the device
        } else if (!mAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
            for (BluetoothDevice dev : bondedDevices) {
                if (dev.getName().contains("IntelliRoast")) {
                    mDevice = dev;
                    break;
                }
            }
            if (mDevice == null) {
                //Not paired to bluetooth
            } else {
                mService.connect(mDevice);

            }
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so connect to IntelliRoast
                    connectToIntelliRoast();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                }
        }
    }

    public class ConnectionHandler extends Handler {
        private MainActivity activity;
        ConnectionHandler(MainActivity displayActivity) {
            activity = displayActivity;
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case constants.MESSAGE_STATE_CHANGE:
                    int state = mService.getState();
                    if (state == mService.STATE_CONNECTED){
                        showToast("IntelliRoast Connected");
                        isConnected = true;
                    } else if (state == mService.STATE_CONNECTING) {
                        showToast("Connecting to IntelliRoast");
                    } else if (state == mService.STATE_NONE) {
                        showToast("IntelliRoast Disconnected");
                        isConnected = true;
                    }
                    break;
                case constants.MESSAGE_MACHINE_STATE:
                    String roastDetails;
                    if (mMachineState.roastState.contains("Idle")) {
                        roastDetails = "IntelliRoast is currently waiting to roast.";
                        if (!mMachineState.timeElapsed.equals("")) {
                            String timeString;
                            int time = Integer.parseInt(mMachineState.timeElapsed);
                            if(time > 59) {
                                int minutes = time / 60;
                                int seconds = time % 60;
                                timeString = Integer.toString(minutes) + "m " + Integer.toString(seconds) + "s.";
                            } else {
                                timeString = mMachineState.timeElapsed+"s.";
                            }
                            if (Integer.parseInt(mMachineState.timeElapsed) > 0) {
                                roastDetails = "Last Roast took " + timeString +
                                        "\nThe beans reached a maximum temperature of "
                                        + mMachineState.maxBeanTemp + " C." +
                                        "\nIntelliRoast is currently waiting to roast.";
                            }
                        }
                        mRoastDetails.setText(roastDetails);
                    } else {
                        String timeString;
                        int time = Integer.parseInt(mMachineState.timeElapsed);
                        if(time > 59) {
                            int minutes = time / 60;
                            int seconds = time % 60;
                            timeString = Integer.toString(minutes) + "m " + Integer.toString(seconds) + "s";
                        } else {
                            timeString = mMachineState.timeElapsed+"s";
                        }
                        if (isManual) {
                            roastDetails = "IntelliRoast is currently " + mMachineState.roastState + "." +
                                    "\nTime Elapsed: " + timeString +
                                    "\nBean Temp: " + mMachineState.beanTemp + " C" +
                                    "\nSet Temp: " + mMachineState.setTemp + " C" +
                                    "\nExhaust Air Temp: " + mMachineState.exhaustTemp + " C" +
                                    "\nInput Air Temp: " + mMachineState.inputTemp + " C" +
                                    "\nHeating Element Power: " + mMachineState.elementPower + "%" +
                                    "\nFan Speed: " + mMachineState.fanSpeed + "%";
                        }
                    }
                    break;
                case constants.MESSAGE_READ:
                    String message = (String) msg.obj;
                    Log.d(TAG, message);
                    break;
                case constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
        //toast message function
        private void showToast(String msg){
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onClick(View v) {

        int returnCode;
        switch (v.getId()) {
            case R.id.doLight:
                // Load Light Roast
                if (mService.getState() != mService.STATE_CONNECTED) {
                    showToast("Not connected to IntelliRoast");
                    break;
                }
                mService.loadRoast("Light");
                break;
            case R.id.doMedium:
                // Load Medium Roast
                if (mService.getState() != mService.STATE_CONNECTED) {
                    showToast("Not connected to IntelliRoast");
                    break;
                }
                mService.loadRoast("Medium");
                break;
            case R.id.doDark:
                // Load Dark Roast
                if (mService.getState() != mService.STATE_CONNECTED) {
                    showToast("Not connected to IntelliRoast");
                    break;
                }
                mService.loadRoast("Dark");
                break;
            case R.id.startRoast:
                // Start roast
                if (mService.getState() != mService.STATE_CONNECTED) {
                    showToast("Not connected to IntelliRoast");
                    break;
                }
                if (isManual) {
                    showToast("You must stop your Manual Roast first!");
                    break;
                }
                mMachineState.maxBeanTemp = 0;
                mMachineState.roastTime = 0;
                mService.startRoast();
                break;
            case R.id.endManual:
                // Go back to Auto
                if (mService.getState() != mService.STATE_CONNECTED) {
                    showToast("Not connected to IntelliRoast");
                    break;
                }
                mService.stopRoast();
                isManual = false;
                break;
            case R.id.startManual:
                // Start a Manual Roast
                if (mService.getState() != mService.STATE_CONNECTED) {
                    showToast("Not connected to IntelliRoast");
                    return;
                }
                String fanSpeed = mFanSpeed.getSelectedItem().toString();
                String power = mPower.getSelectedItem().toString();
                String manualCommand = "{\"cmd\":\"Manual\",\"fan\":\"" +
                        fanSpeed + "\",\"power\":\"" +
                        power + "\"}";
                mMachineState.maxBeanTemp = 0;
                mMachineState.roastTime = 0;
                mService.startManualMode(manualCommand);
                isManual = true;
                break;
        }
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
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void showToastShort(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        unbindService(connection);
        isManual = false;
        super.onDestroy();
    }

}
