package com.neubofy.veto.commands

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.permissions.LocationPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.NetworkUtils
import com.neubofy.veto.utils.WifiScan
import com.neubofy.veto.utils.getSsidCompat
import kotlinx.coroutines.CompletableDeferred


class StatsCommand(context: Context) : Command(context) {

    override val keyword = "stats"
    override val usage = "stats"

    @get:DrawableRes
    override val icon = R.drawable.ic_cell_wifi

    @get:StringRes
    override val shortDescription = R.string.cmd_stats_description_short

    override val longDescription = R.string.cmd_stats_description_long

    override val requiredPermissions = listOf(LocationPermission())

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val ips = NetworkUtils.getIps(context)
        val ipsString = ips.joinToString(", ")

        // Hardware details
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        val androidVersion = android.os.Build.VERSION.RELEASE
        val sdkLevel = android.os.Build.VERSION.SDK_INT

        // Battery details
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val batteryPct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)

        // Telephony details (SIM/IMEI)
        var networkOperator = "Unknown"
        
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        try {
            networkOperator = tm.networkOperatorName ?: "Unknown"
        } catch (e: Exception) {
            // Ignore
        }

        val deferred = CompletableDeferred<List<android.net.wifi.ScanResult>>()

        WifiScan(context) { scanResults ->
            deferred.complete(scanResults)
        }.startWifiScan()

        // Wait up to 5 seconds for WiFi scan results
        val scanResults = kotlinx.coroutines.withTimeoutOrNull(5000L) {
            deferred.await()
        } ?: emptyList()

        val wifisString =
            scanResults.joinToString(", ") { sr -> "${sr.getSsidCompat()}" }

        val reply = """
            Model: $manufacturer $model
            OS: Android $androidVersion (SDK $sdkLevel)
            Battery: $batteryPct%
            SIM Network: $networkOperator
            IPs: $ipsString
            WiFi: ${wifisString.ifEmpty { "Unavailable/Timed out" }}
        """.trimIndent()

        transport.send(context, reply, keyword)
    }
}
