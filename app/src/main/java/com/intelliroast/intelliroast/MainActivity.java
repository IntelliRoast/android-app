package com.intelliroast.intelliroast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.CharBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BluetoothClient client;

    private static final int REQUEST_ENABLE_BT = 1;
    final UUID sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    final String connected = "{\"cmd\":\"Connected\"}";
    final String connectedAck = "{\"cmd\":\"Ack\",\"state\":\"Idle\"}";
    final String startCommand = "{\"cmd\":\"Start\"}";
    final String stopCommand = "{\"cmd\":\"Stop\"}";
    final String ejectCommand = "{\"cmd\":\"Eject\"}";

    private DrawerLayout mDrawerLayout;

    BluetoothAdapter bluetoothAdapter;

    Button chooseLightRoast;
    Button chooseMediumRoast;
    Button chooseDarkRoast;
    FloatingActionButton startRoast;

    public static Boolean isConnected = false;

    public String roastType = "Medium";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                            case R.id.menu_dev:
                                openDevOptions();
                                return true;
                        }
                        return true;
                    }
                });


        chooseLightRoast.setOnClickListener(this);
        chooseMediumRoast.setOnClickListener(this);
        chooseDarkRoast.setOnClickListener(this);
        startRoast.setOnClickListener(this);
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
                byte[] readBuf = (byte[]) msg.obj;
                String message = new String(readBuf, 0, msg.arg1);

                // Convert to JSON
                JSONObject messageReceived;
                try {
                    messageReceived = new JSONObject(message);
                    switch ((String) messageReceived.get("state")) {
                        case "Roasting":
                            showToast((String) messageReceived.get("T"));
                            return;
                    }
                } catch (org.json.JSONException ex) {
                    return;
                }


            }
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
                loadRoast(roastType);
        }
        //Your Logic
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
        showToast("Starting " + roastType + " Roast!");
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
        showToast("Stopping the current roast");
        client.write(stopCommand.getBytes());
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
    //toast message function
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
