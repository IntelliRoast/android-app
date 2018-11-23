package com.intelliroast.intelliroast;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

    final UUID sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    final String connected = "{\"cmd\":\"Connected\"}";
    final String connectedAck = "{\"cmd\":\"Ack\",\"state\":\"Idle\"}";
    final String startCommand = "{\"cmd\":\"Start\"}";

    private DrawerLayout mDrawerLayout;

    BluetoothAdapter bluetoothAdapter;
    public BluetoothSocket bluetoothSocket;

    Button chooseLightRoast;
    Button chooseMediumRoast;
    Button chooseDarkRoast;
    FloatingActionButton startRoast;

    Boolean isConnected = false;

    public String roastProfile = "{\"cmd\":\"Load\",\"default\":2}";
    public String roastType = "Medium";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            showToast("Bluetooth not supported!");
            finish();
        }

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
                        // set item as selected to persist highlight
//                        menuItem.setChecked(true);
                        // close drawer when item is tapped
//                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here
                        switch (menuItem.getItemId()) {
                            case R.id.menu_connect:
                                connectToIntelliRoast();
                                return true;
                        }

                        return true;
                    }
                });
        bluetoothSocket = connectToIntelliRoast();
        if (bluetoothSocket != null) {
            try {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bluetoothSocket.getOutputStream(), "ASCII"));
                writer.write(connected);
                writer.flush();
            } catch (IOException ex) {

            }

            CharBuffer cb = CharBuffer.allocate(100);
            String output = "";
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(bluetoothSocket.getInputStream(), "ASCII"));
                while (reader.ready()) {
                    reader.read(cb);
                    cb.flip();
                    output = cb.toString();
                }
            } catch (IOException ex) {
                return;
            }
            if (output.contains("Ack")) {
                isConnected = true;
                showToast("Connected to IntelliRoast");
            } else {
                isConnected = true;
                showToast("Error connecting to IntelliRoast");
            }
        }

        chooseLightRoast.setOnClickListener(this);
        chooseMediumRoast.setOnClickListener(this);
        chooseDarkRoast.setOnClickListener(this);
        startRoast.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.doLight:
                // Load Light Roast
                loadRoast("light");
                break;
            case R.id.doMedium:
                // Load Medium Roast
                loadRoast("medium");
                break;
            case R.id.doDark:
                // Load Dark Roast
                loadRoast("dark");
                break;
            case R.id.startRoast:
                // Start roast
                beginRoast();
        }
        //Your Logic
    }

    public void loadRoast(String type) {
        String roastProfile;
        switch (type) {
            case "light":
                roastType = "Light";
                roastProfile = "{\"cmd\":\"Load\",\"default\":1}";
                break;
            case "medium":
                roastType = "Medium";
                roastProfile = "{\"cmd\":\"Load\",\"default\":2}";
                break;
            case "dark":
                roastType = "Dark";
                roastProfile = "{\"cmd\":\"Load\",\"default\":3}";
                break;
            default:
                roastProfile = "Something broke!!!";
                break;
        }
        if (bluetoothSocket != null) {
            try {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bluetoothSocket.getOutputStream(), "ASCII"));
                writer.write(roastProfile);
                writer.flush();
                showToast(roastType + " Profile Loaded!");
            } catch (IOException ex) {
                showToast("Could not send Profile");
            }
        }
    }

    public void beginRoast() {
        if (roastProfile != "") {
            if (bluetoothSocket != null) {
                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bluetoothSocket.getOutputStream(), "ASCII"));
                    writer.write(startCommand);
                    writer.flush();
                    showToast("Starting " + roastType + " Roast!");
                } catch (IOException ex) {
                    showToast("Could not send Profile");
                }
            } else {
                showToast("Bluetooth not connected");
            }
        } else {
            showToast("Please select a roast first!");
        }
    }

    // Set up Bluetooth connection, called from sidebar
    public BluetoothSocket connectToIntelliRoast() {
        BluetoothDevice bluetoothDevice = null;
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice dev : bondedDevices) {
            if (dev.getName().equals("IntelliRoast")) {
                bluetoothDevice = dev;
                break;
            }
        }
        if (bluetoothDevice == null) {
            showToast("Target Bluetooth device is not found.");
            return null;
        }

        BluetoothSocket bluetoothSocket;
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(sppUuid);
        } catch (IOException ex) {
            showToast("Failed to connect: " + ex.toString());
            return null;
        }

        try {
            bluetoothSocket.connect();
        } catch (IOException ex) {
            showToast("Failed to connect: " + ex.toString());
            return null;
        }
        return bluetoothSocket;
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

//    @Override
//    public void onResume(){
//        super.onResume();
//        if (bluetoothSocket == null) {
//            bluetoothSocket = connectToIntelliRoast();
//        }
//    }
}
