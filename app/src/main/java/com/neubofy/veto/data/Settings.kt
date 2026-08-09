package com.neubofy.veto.data

import com.neubofy.veto.utils.RingerUtils

class Settings : HashMap<Int, Any>() {

    companion object {
        const val SET_Veto_COMMAND = 1
        const val SET_PIN = 2
        const val SET_WIPE_ENABLED = 3
        const val SET_ACCESS_VIA_PIN = 4
        const val SET_FIRST_TIME_WHITELIST = 5
        const val SET_FIRST_TIME_CONTACT_ADDED = 6
        const val SET_VetoSERVER_ID = 8
        const val SET_VetoSERVER_PASSWORD_SET = 9
        const val SET_VetoSERVER_URL = 10
        const val SET_VetoSERVER_PUSH_URL = 11
        const val SET_VetoSERVER_UPDATE_TIME = 13
        const val SET_SYNCED_FCM_TOKEN = 14
        const val SET_LOCKSCREEN_MESSAGE = 15

        const val SET_THEME = 18
        const val VAL_THEME_FOLLOW_SYSTEM = "follow_system"
        const val VAL_THEME_DARK = "dark"
        const val VAL_THEME_LIGHT = "light"
        const val VAL_THEME_BATTERY_SAVER = "battery_saver"

        const val SET_RINGER_TONE = 19

        const val SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED = 22
        const val SET_Veto_EDGE_INFO_SHOWN = 23
        const val SET_Veto_CRYPT_HPW = 24
        const val SET_Veto_CRYPT_PRIVKEY = 25
        const val SET_Veto_CRYPT_PUBKEY = 26

        const val SET_DYNAMIC_COLORS = 27
        const val SET_CUSTOM_COLOR = 28

        const val SET_SET_VERSION = 500
        const val SET_APP_CRASHED_LOG_ENTRY = 501

        const val SET_THEFT_MODE_ACTIVE = 701
        const val SET_THEFT_MODE_PIN = 702

        const val SET_AUTO_THEFT_ENABLED = 703
        const val SET_AUTO_THEFT_SIM_REMOVED = 704
        const val SET_AUTO_THEFT_FAILED_UNLOCK = 705
        const val SET_AUTO_THEFT_MAX_ATTEMPTS = 706
        const val SET_AUTO_THEFT_PROOF_UNLOCK = 707
        const val SET_AUTO_THEFT_PROOF_CHARGE = 708
        const val SET_AUTO_THEFT_PROOF_SIM = 709
        const val SET_AUTO_THEFT_LOCK_MSG = 710
        const val SET_AUTO_THEFT_OWNER_SIM = 711
        
        const val SET_AUTO_THEFT_WARNING_ACTIVE = 712
        const val SET_RING_LOCK_ENABLED = 713
        const val SET_AUTO_THEFT_FAILED_COUNTER = 714

        // New Beta & Defense keys
        const val SET_AUTO_THEFT_BETA_FAILED_UNLOCK = 715
        const val SET_AUTO_THEFT_CUSTOM_TTS = 716
        const val SET_AUTO_THEFT_CONTACT_PHONE = 717
        const val SET_AUTO_THEFT_CONTACT_EMAIL = 718
        const val SET_AUTO_THEFT_CONTACT_SOCIAL = 719
        const val SET_AUTO_THEFT_LAST_BAD_EVENT_TIME = 720
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
            SET_DYNAMIC_COLORS,
            SET_AUTO_THEFT_ENABLED,
            SET_AUTO_THEFT_SIM_REMOVED,
            SET_AUTO_THEFT_FAILED_UNLOCK,
            SET_AUTO_THEFT_PROOF_CHARGE,
            SET_AUTO_THEFT_WARNING_ACTIVE,
            SET_AUTO_THEFT_BETA_FAILED_UNLOCK,
            SET_RING_LOCK_ENABLED -> false

            SET_AUTO_THEFT_PROOF_UNLOCK,
            SET_AUTO_THEFT_PROOF_SIM -> true

            SET_Veto_COMMAND -> "veto"
            SET_VetoSERVER_UPDATE_TIME -> 60
            SET_SET_VERSION,
            SET_APP_CRASHED_LOG_ENTRY,
            SET_AUTO_THEFT_FAILED_COUNTER -> 0
            SET_AUTO_THEFT_LAST_BAD_EVENT_TIME -> 0L

            SET_AUTO_THEFT_MAX_ATTEMPTS -> 3

            SET_RINGER_TONE -> RingerUtils.getDefaultRingtoneAsString()

            SET_AUTO_THEFT_CUSTOM_TTS -> "Theft suspected. Please unlock device to verify ownership."

            SET_PIN,
            SET_LOCKSCREEN_MESSAGE,
            SET_VetoSERVER_ID,
            SET_SYNCED_FCM_TOKEN,
            SET_Veto_CRYPT_HPW,
            SET_Veto_CRYPT_PRIVKEY,
            SET_Veto_CRYPT_PUBKEY,
            SET_THEFT_MODE_PIN,
            SET_VetoSERVER_PUSH_URL,
            SET_AUTO_THEFT_LOCK_MSG,
            SET_AUTO_THEFT_OWNER_SIM,
            SET_AUTO_THEFT_CONTACT_PHONE,
            SET_AUTO_THEFT_CONTACT_EMAIL,
            SET_AUTO_THEFT_CONTACT_SOCIAL -> ""

            SET_VetoSERVER_URL -> "https://veto.neubofy.in"

            SET_THEME -> VAL_THEME_FOLLOW_SYSTEM
            SET_CUSTOM_COLOR -> 0xD4AF37
            else -> ""
        }
    }
}
