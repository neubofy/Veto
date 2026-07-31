package com.neubofy.veto.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager

data class NetworkInfo(
    val interfaceName: String,
    val linkAddresses: List<String>,
) {
    override fun toString(): String {
        return "Interface: $interfaceName\n" + linkAddresses.joinToString("\n")
    }
}

object NetworkUtils {

    // https://developer.android.com/develop/connectivity/network-ops/reading-network-state
    fun getIps(context: Context): List<NetworkInfo> {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

        @Suppress("Deprecation")
        return connectivityManager.allNetworks
            .map { network -> connectivityManager.getLinkProperties(network) }
            .filterNotNull()
            .map { linkProps ->
                NetworkInfo(
                    linkProps.interfaceName ?: "",
                    linkProps.linkAddresses.map { addr -> addr.toString() }.toList()
                )
            }
            .toList()
    }

    @JvmStatic
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

        val activeNetwork = connectivityManager.activeNetwork ?: return false

        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
