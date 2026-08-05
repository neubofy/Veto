package com.neubofy.veto.data

import androidx.annotation.Keep
import com.neubofy.veto.utils.RingerUtils

@Keep
class Settings : HashMap<Int, Any>() {

    companion object {
        const val SETTINGS_VERSION = 3

        const val SET_WIPE_ENABLED = 0
        const val SET_ACCESS_VIA_PIN = 1
        const val SET_LOCKSCREEN_MESSAGE = 2
        const val SET_PIN = 3
        const val SET_Veto_COMMAND = 4
        const val SET_RINGER_TONE = 7
        const val SET_SET_VERSION = 8

        const val SET_VetoSERVER_URL = 102
        const val SET_VetoSERVER_UPDATE_TIME = 103
        const val SET_VetoSERVER_ID = 104
        const val SET_SYNCED_FCM_TOKEN = 1041
        const val SET_VetoSERVER_PASSWORD_SET = 105
        const val SET_Veto_CRYPT_PUBKEY = 108
        const val SET_Veto_CRYPT_PRIVKEY = 109
        const val SET_Veto_CRYPT_HPW = 110
        const val SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED = 113
        const val SET_VetoSERVER_PUSH_URL = 114
        const val SET_Veto_EDGE_INFO_SHOWN = 117

        const val SET_FIRST_TIME_WHITELIST = 301
        const val SET_FIRST_TIME_CONTACT_ADDED = 302

        const val SET_APP_CRASHED_LOG_ENTRY = 401

        const val SET_THEME = 601
        const val VAL_THEME_FOLLOW_SYSTEM = "follow_system"
        const val VAL_THEME_LIGHT = "light"
        const val VAL_THEME_DARK = "dark"
        const val SET_DYNAMIC_COLORS = 602
        const val SET_CUSTOM_COLOR = 603
        const val SET_THEFT_MODE_ACTIVE = 701
        const val SET_THEFT_MODE_PIN = 702
    }

    override fun get(key: Int): Any {
        if (super.containsKey(key)) {
            return super.get(key)!!
        }
        return when (key) {
            SET_WIPE_ENABLED,
            SET_ACCESS_VIA_PIN,
            SET_FIRST_TIME_WHITELIST,
            SET_FIRST_TIME_CONTACT_ADDED,
            SET_VetoSERVER_PASSWORD_SET,
            SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED,
            SET_Veto_EDGE_INFO_SHOWN,
            SET_THEFT_MODE_ACTIVE,
            SET_DYNAMIC_COLORS -> false

            SET_Veto_COMMAND -> "veto"
            SET_VetoSERVER_UPDATE_TIME -> 60
            SET_SET_VERSION,
            SET_APP_CRASHED_LOG_ENTRY -> 0

            SET_RINGER_TONE -> RingerUtils.getDefaultRingtoneAsString()

            SET_PIN,
            SET_LOCKSCREEN_MESSAGE,
            SET_VetoSERVER_ID,
            SET_SYNCED_FCM_TOKEN,
            SET_Veto_CRYPT_HPW,
            SET_Veto_CRYPT_PRIVKEY,
            SET_Veto_CRYPT_PUBKEY,
            SET_THEFT_MODE_PIN,
            SET_VetoSERVER_PUSH_URL -> ""

            SET_VetoSERVER_URL -> "https://veto.neubofy.in"

            SET_THEME -> VAL_THEME_FOLLOW_SYSTEM
            SET_CUSTOM_COLOR -> 0xD4AF37
            else -> ""
        }
    }
}
