package com.neubofy.veto.utils

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build

fun ScanResult.getSsidCompat(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.wifiSsid.toString()
    } else {
        return this.SSID
    }
}

class WifiScan(
    private val context: Context,
    private val onScanResults: (List<ScanResult>) -> Unit,
) {

    companion object {
        private val TAG = WifiScan::class.simpleName
    }

    private val wifiManager = context.applicationContext.getSystemService(WifiManager::class.java)

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            context.log()
                .i(TAG, "Received SCAN_RESULTS_AVAILABLE_ACTION broadcast. Success=$success.")
            onResult()
        }
    }

    fun startWifiScan() {
        // TODO(220): Use Device Owner Permission to auto-enable and auto-disable WiFi
        // wifiManager.setWifiEnabled(true)

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)

        // TODO: This is subject to throttling: https://developer.android.com/develop/connectivity/wifi/wifi-scan#wifi-scan-throttling
        // Therefore we need to become a foreground app (== have a foreground service...)
        val success = wifiManager.startScan()
        context.log().i(TAG, "Started WiFi scan")

        if (!success) {
            context.log().i(TAG, "Starting WiFi scan failed. Using cached data.")
            onResult()
        }
    }

    @SuppressLint("MissingPermission") // ACCESS_FINE_LOCATION
    private fun onResult() {
        context.unregisterReceiver(this.wifiScanReceiver)
        onScanResults(wifiManager.scanResults)
    }
}
