package com.example.wifimanager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import androidx.core.content.ContextCompat;

public class WifiManagerHelper {

    private final WifiManager wifiManager;
    private final Context context;

    public WifiManagerHelper(Context context) {
        this.context = context.getApplicationContext();
        wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
    }

    public boolean isWifiEnabled() {
        return wifiManager.isWifiEnabled();
    }

    public void setWifiEnabled(boolean enabled) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
            wifiManager.setWifiEnabled(enabled);
        }
    }
}
