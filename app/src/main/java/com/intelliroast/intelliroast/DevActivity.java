package com.intelliroast.intelliroast;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.json.JSONObject;

public class DevActivity extends AppCompatActivity implements View.OnClickListener {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dev_options);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_back);

        DrawerLayout mDrawerLayout;

        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        goBackHome();
                        return true;
                    }
                });
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
                            showToast((String) messageReceived.get("BT"));
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
            default:
                return;
        }
    }

    public void goBackHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    //toast message function
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
