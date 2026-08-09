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

}
