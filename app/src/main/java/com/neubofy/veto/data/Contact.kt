package com.neubofy.veto.data

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import androidx.annotation.Keep

@Keep
data class Contact(
    var name: String,
    var number: String
) {
    companion object {
        @JvmStatic
        fun from(context: Context, name: String, number: String): Contact? {
            val tm = context.getSystemService(TelephonyManager::class.java)
            val iso = tm?.networkCountryIso ?: ""

            val numberFormatted = if (iso.isEmpty()) {
                @Suppress("DEPRECATION")
                PhoneNumberUtils.formatNumber(number)
            } else {
                PhoneNumberUtils.formatNumber(number, iso)
            }

            if (numberFormatted.isNullOrBlank()) {
                return null
            }

            return Contact(name, numberFormatted)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Contact) return false
        return PhoneNumberUtils.compare(number, other.number)
    }

    override fun hashCode(): Int {
        return number.hashCode()
    }
}
