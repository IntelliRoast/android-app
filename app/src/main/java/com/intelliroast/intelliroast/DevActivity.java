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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import static com.intelliroast.intelliroast.MainActivity.beanTemp;

public class DevActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "DevUI";
    DrawerLayout mDrawerLayout;
    TextView mBeanTemp;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dev_options);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_back);


        mDrawerLayout = findViewById(R.id.drawer_layout);
        mBeanTemp = findViewById(R.id.bean_temp);

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

    public void updateBeanTemp(String text) {
        mBeanTemp.setText(text);
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
