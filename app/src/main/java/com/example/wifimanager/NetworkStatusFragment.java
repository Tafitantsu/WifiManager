package com.example.wifimanager;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.Objects;

public class NetworkStatusFragment extends Fragment {

    private TextView textViewStatus;
    private WifiManager wifiManager;
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWifiStatus();
        }
    };

    public NetworkStatusFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wifiManager = (WifiManager) requireActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_network_status, container, false);
        textViewStatus = view.findViewById(R.id.textViewStatus);

        updateWifiStatus(); // Initial status update

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register BroadcastReceiver to monitor WiFi state changes
        requireActivity().registerReceiver(wifiStateReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        requireActivity().registerReceiver(wifiStateReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister BroadcastReceiver to avoid memory leaks
        requireActivity().unregisterReceiver(wifiStateReceiver);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void updateWifiStatus() {
        WifiInfo currentWifiInfo = wifiManager.getConnectionInfo();
        if (currentWifiInfo != null && currentWifiInfo.getNetworkId() != -1) {
            String ssid = currentWifiInfo.getSSID();
            int ipAddress = currentWifiInfo.getIpAddress();
            String ipString = String.format("%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff));

            // Signal strength (RSSI) in dBm
            int rssi = currentWifiInfo.getRssi();
            int level = WifiManager.calculateSignalLevel(rssi, 5); // Convert RSSI to level out of 5
            String signalStrength = String.format("Signal Strength: %d dBm (Level %d/5)", rssi, level);

            // Security type
            String securityType = getSecurityType(currentWifiInfo);

            // DNS servers
            String dnsServers = getDnsServers();

            // Build the status message
            String statusMessage = String.format("WiFi Status: Connected to %s\n", ssid) +
                    String.format("IP Address: %s\n", ipString) +
                    signalStrength + "\n" +
                    "Security Type: " + securityType + "\n" +
                    "DNS Servers: " + dnsServers;

            textViewStatus.setText(statusMessage);
        } else {
            textViewStatus.setText("Please connect to available networks");
        }
    }

    private String getSecurityType(WifiInfo wifiInfo) {
        if (wifiInfo.getSupplicantState() == null) {
            return "Unknown";
        }
        String auth = wifiInfo.getSupplicantState().toString();
        switch (auth) {
            case "COMPLETED":
                return "WPA2";
            case "ASSOCIATED":
                return "WPA";
            case "AUTHENTICATING":
                return "WEP";
            case "GROUP_HANDSHAKE":
                return "802.1x";
            default:
                return "Open";
        }
    }

    private String getDnsServers() {
        StringBuilder dnsServers = new StringBuilder();
        ConnectivityManager connectivityManager = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                String[] dnsServersArray = Objects.requireNonNull(connectivityManager.getLinkProperties(activeNetwork)).getDnsServers().toString().split(",");
                dnsServers.append(TextUtils.join(", ", dnsServersArray));
            }
        }
        return dnsServers.toString();
    }
}
