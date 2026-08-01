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
import com.neubofy.veto.utils.log
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

    override val optionalPermissions = listOf(com.neubofy.veto.permissions.PhoneStatePermission())

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

        // Telephony details (Multi-SIM & eSIM)
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        val mainOperator = try { tm.networkOperatorName.takeIf { !it.isNullOrBlank() } ?: "Unknown" } catch (_: Exception) { "Unknown" }

        val simLines = mutableListOf<String>()
        val phonePerm = com.neubofy.veto.permissions.PhoneStatePermission()
        if (phonePerm.isGranted(context)) {
            try {
                val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
                @Suppress("MissingPermission")
                val activeList = sm?.activeSubscriptionInfoList
                if (!activeList.isNullOrEmpty()) {
                    for ((index, info) in activeList.withIndex()) {
                        val carrierName = info.carrierName?.toString()?.takeIf { it.isNotBlank() } ?: mainOperator
                        var num: String? = null
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            num = sm.getPhoneNumber(info.subscriptionId)
                        }
                        if (num.isNullOrBlank()) {
                            @Suppress("DEPRECATION")
                            num = info.number
                        }
                        val finalNum = if (!num.isNullOrBlank()) num else "Number unavailable (Carrier restricted)"
                        simLines.add("SIM ${index + 1} ($carrierName): $finalNum")
                    }
                }
            } catch (e: Exception) {
                context.log().e("StatsCommand", "Error reading SIM phone number: ${e.message}")
            }
        }
        if (simLines.isEmpty()) {
            try {
                @Suppress("DEPRECATION", "MissingPermission")
                val line1 = tm.line1Number
                if (!line1.isNullOrBlank()) {
                    simLines.add("SIM 1 ($mainOperator): $line1")
                }
            } catch (_: Exception) {}
        }
        if (simLines.isEmpty()) {
            simLines.add("SIM 1 ($mainOperator): Not available (Permission / Carrier blank)")
        }

        val simFormattedOutput = simLines.joinToString("\n")

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
            $simFormattedOutput
            IPs: $ipsString
            WiFi: ${wifisString.ifEmpty { "Unavailable/Timed out" }}
        """.trimIndent()

        transport.send(context, reply, keyword)
    }
}
