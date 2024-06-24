package com.example.wifimanager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.util.List;

public class AvailableNetworksFragment extends Fragment {

    private WifiManager wifiManager;
    private ArrayAdapter<String> adapter;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Handler handler;
    private Runnable scanRunnable;
    private String targetSSID;

    public AvailableNetworksFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wifiManager = (WifiManager) requireActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        scanRunnable = new Runnable() {
            @Override
            public void run() {
                scanWifiNetworks();
                handler.postDelayed(this, 3000); // Schedule the scan every 3 seconds
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_available_networks, container, false);
        ListView listViewAvailableNetworks = view.findViewById(R.id.listViewAvailableNetworks);

        // Initialize the adapter for list view
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
        listViewAvailableNetworks.setAdapter(adapter);

        // Set item click listener for list view
        listViewAvailableNetworks.setOnItemClickListener((parent, view1, position, id) -> {
            String selectedNetwork = adapter.getItem(position);
            if (selectedNetwork != null) {
                attemptToConnect(selectedNetwork);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check for location permission
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Start scanning for WiFi networks
            handler.post(scanRunnable); // Start the periodic scanning
        }
        // Register the receiver for connectivity changes
        requireContext().registerReceiver(wifiReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop WiFi scanning to conserve battery when the fragment is not visible
        handler.removeCallbacks(scanRunnable);
        // Unregister the receiver
        requireContext().unregisterReceiver(wifiReceiver);
    }

    @SuppressLint("MissingPermission")
    private void scanWifiNetworks() {
        wifiManager.startScan();
        List<ScanResult> results = wifiManager.getScanResults();
        adapter.clear();
        for (ScanResult result : results) {
            // Filter out networks with empty SSID and add only SSIDs
            if (result.SSID != null && !result.SSID.isEmpty()) {
                adapter.add(result.SSID);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void attemptToConnect(String ssid) {
        // Store the target SSID
        targetSSID = ssid;

        // Check if the device is already connected to this network
        String currentSSID = wifiManager.getConnectionInfo().getSSID();
        if (currentSSID.equals("\"" + ssid + "\"")) {
            Toast.makeText(requireContext(), "Already connected to " + ssid, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if the network is already configured
        @SuppressLint("MissingPermission") List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration config : configuredNetworks) {
                if (config.SSID != null && config.SSID.equals("\"" + ssid + "\"")) {
                    // Disconnect from current network
                    wifiManager.disconnect();
                    // Set the priority of this network to a high value
                    config.priority = 999;
                    wifiManager.updateNetwork(config);
                    // Connect to the selected network
                    wifiManager.enableNetwork(config.networkId, true);
                    wifiManager.reconnect();
                    Toast.makeText(requireContext(), "Connecting to " + ssid, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        // Network not configured, show dialog to enter PSK
        showPskDialog(ssid);
    }

    private void showPskDialog(String ssid) {
        if (ssid == null) return; // Prevent showing the dialog with null SSID

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Password for " + ssid);

        View viewInflated = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_psk_input, null);
        TextView textViewNetworkName = viewInflated.findViewById(R.id.textViewNetworkName);
        textViewNetworkName.setText(ssid);

        EditText editTextPassword = viewInflated.findViewById(R.id.editTextPsk);

        builder.setView(viewInflated);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String psk = editTextPassword.getText().toString().trim();
            if (psk.isEmpty()) {
                Toast.makeText(requireContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show();
            } else {
                connectToWifi(ssid, psk);
            }
        });

        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void connectToWifi(String ssid, String psk) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", psk);
        wifiConfig.priority = 999; // Set high priority for this network
        int networkId = wifiManager.addNetwork(wifiConfig);
        if (networkId == -1) {
            Toast.makeText(requireContext(), "Failed to add network configuration", Toast.LENGTH_SHORT).show();
            return;
        }
        wifiManager.disconnect();
        boolean enabled = wifiManager.enableNetwork(networkId, true);
        if (!enabled) {
            Toast.makeText(requireContext(), "Failed to enable network", Toast.LENGTH_SHORT).show();
            return;
        }
        wifiManager.reconnect();
        Toast.makeText(requireContext(), "Connecting to " + ssid, Toast.LENGTH_SHORT).show();
    }

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null && networkInfo.isConnected()) {
                String connectedSSID = wifiManager.getConnectionInfo().getSSID();
                if (!connectedSSID.equals("\"" + targetSSID + "\"")) {
                    // Not connected to the desired SSID, attempt to connect again
                    attemptToConnect(targetSSID);
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handler.post(scanRunnable); // Start the periodic scanning if permission granted
            } else {
                Toast.makeText(requireContext(), "Location permission is required to scan WiFi networks", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
