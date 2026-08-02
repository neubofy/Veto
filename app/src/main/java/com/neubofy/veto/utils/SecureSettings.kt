package com.neubofy.veto.utils

import android.content.Context
import android.provider.Settings
import com.neubofy.veto.utils.log

object SecureSettings {
    private val TAG = SecureSettings::class.java.simpleName

    @JvmStatic
    fun turnGPS(context: Context, enable: Boolean) {
        @Suppress("DEPRECATION")
        val value = if (enable) {
            Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
        } else {
            Settings.Secure.LOCATION_MODE_OFF
        }
        @Suppress("DEPRECATION")
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.LOCATION_MODE,
            value.toString()
        )
        context.log().d(TAG, "Turned GPS on/off using SecureSettings: $enable")
    }

    @JvmStatic
    fun setBluetooth(context: Context, enable: Boolean): Boolean {
        return try {
            val value = if (enable) 1 else 0
            Settings.Global.putInt(context.contentResolver, "bluetooth_on", value)
            context.log().d(TAG, "Turned Bluetooth on/off using SecureSettings: $enable")
            true
        } catch (e: Exception) {
            context.log().e(TAG, "Failed to toggle bluetooth via SecureSettings: ${e.message}")
            false
        }
    }
}
