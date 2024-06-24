package com.example.wifimanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class RegisteredNetworksActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ArrayAdapter<WifiConfiguration> adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registered_networks);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        ListView listViewRegisteredNetworks = findViewById(R.id.listViewRegisteredNetworks);

        adapter = new ArrayAdapter<WifiConfiguration>(this, R.layout.list_item_registered_network) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_registered_network, parent, false);
                }

                WifiConfiguration wifiConfig = getItem(position);

                TextView textViewSSID = convertView.findViewById(R.id.textViewSSID);
                Button buttonDelete = convertView.findViewById(R.id.buttonDelete);

                if (wifiConfig != null) {
                    textViewSSID.setText(wifiConfig.SSID);

                    buttonDelete.setOnClickListener(v -> removeNetwork(wifiConfig.networkId));
                }

                return convertView;
            }
        };

        listViewRegisteredNetworks.setAdapter(adapter);

        loadRegisteredNetworks();
    }

    @SuppressLint("MissingPermission")
    private void loadRegisteredNetworks() {
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            adapter.clear();
            adapter.addAll(configuredNetworks);
            adapter.notifyDataSetChanged();
        }
    }

    private void removeNetwork(int networkId) {
        boolean success = wifiManager.removeNetwork(networkId);
        if (success) {
            wifiManager.saveConfiguration(); // Obsolete for API 26+, but kept for older versions
            Toast.makeText(this, "Network removed successfully", Toast.LENGTH_SHORT).show();
            loadRegisteredNetworks(); // Refresh the list
        } else {
            Toast.makeText(this, "Failed to remove network. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
