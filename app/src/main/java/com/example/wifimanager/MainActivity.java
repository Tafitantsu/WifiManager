package com.example.wifimanager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 123;
    private WifiManagerHelper wifiManagerHelper;
    private SwitchCompat wifiSwitch;
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWifiSwitch();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize WifiManagerHelper
        wifiManagerHelper = new WifiManagerHelper(this);

        // View initializers
        wifiSwitch = findViewById(R.id.switch1);
        Button startScanButton = findViewById(R.id.button);
        Button aboutButton = findViewById(R.id.button2);

        // Check for permissions
        if (!hasPermissions()) {
            requestPermissions();
        } else {
            initializeWifiSwitch();
        }

        // Switch logic
        wifiSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission denied can't change Wi-Fi state", Toast.LENGTH_SHORT).show();
                wifiSwitch.setChecked(!isChecked); // Revert switch state
                return;
            }
            // Toggle Wi-Fi state using WifiManagerHelper
            wifiManagerHelper.setWifiEnabled(isChecked);
        });

        startScanButton.setOnClickListener(v -> {
            try {
                if (!wifiManagerHelper.isWifiEnabled()) {
                    Toast.makeText(MainActivity.this, "Should enable WiFi first", Toast.LENGTH_SHORT).show();
                    wifiSwitch.setChecked(true); // Ensure switch is checked
                } else {
                    Intent intent = new Intent(this, MainWifiManager.class);
                    startActivity(intent);
                }

            } catch (Exception e) {
                Log.e("MainActivity", "Error while starting scan: " + e.getMessage());
                Toast.makeText(MainActivity.this, "Error occurred while starting scan", Toast.LENGTH_SHORT).show();
            }
        });

        aboutButton.setOnClickListener(v -> {
            // Replace the fragment container with the About fragment
            replaceFragment(About.newInstance());
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register BroadcastReceiver to monitor WiFi state changes
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiStateReceiver, intentFilter);

        // Update WiFi switch state when activity resumes
        updateWifiSwitch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister BroadcastReceiver to avoid memory leaks
        unregisterReceiver(wifiStateReceiver);
    }

    private void initializeWifiSwitch() {
        // Set initial state of the switch using WifiManagerHelper
        wifiSwitch.setChecked(wifiManagerHelper.isWifiEnabled());
    }

    private void updateWifiSwitch() {
        // Update WiFi switch state based on current WiFi enabled status
        wifiSwitch.setChecked(wifiManagerHelper.isWifiEnabled());
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_BOOT_COMPLETED) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.RECEIVE_BOOT_COMPLETED
        }, PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                initializeWifiSwitch();
            } else {
                Toast.makeText(this, "Permission denied can't access to wifi manager", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainerView, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }
}
