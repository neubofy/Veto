package com.neubofy.veto.data;


import androidx.annotation.Keep;

import java.util.HashMap;

import com.neubofy.veto.BuildConfig;
import com.neubofy.veto.utils.RingerUtils;


@Keep
public class Settings extends HashMap<Integer, Object> {

    public static final int SETTINGS_VERSION = 3;

    public static final int SET_WIPE_ENABLED = 0;
    public static final int SET_ACCESS_VIA_PIN = 1;
    public static final int SET_LOCKSCREEN_MESSAGE = 2;
    public static final int SET_PIN = 3;
    public static final int SET_Veto_COMMAND = 4;
    //public static final int SET_INTRODUCTION_VERSION = 6;
    public static final int SET_RINGER_TONE = 7;
    public static final int SET_SET_VERSION = 8;

    //public static final int SET_VetoSERVER_UPLOAD_SERVICE = 101;
    public static final int SET_VetoSERVER_URL = 102;
    public static final int SET_VetoSERVER_UPDATE_TIME = 103;
    public static final int SET_VetoSERVER_ID = 104;
    public static final int SET_SYNCED_FCM_TOKEN = 1041;
    public static final int SET_VetoSERVER_PASSWORD_SET = 105;
    public static final int SET_VetoSERVER_LOCATION_TYPE = 106; // BackgroundLocationType
    //public static final int SET_VetoSERVER_AUTO_UPLOAD = 107;
    public static final int SET_Veto_CRYPT_PUBKEY = 108;
    public static final int SET_Veto_CRYPT_PRIVKEY = 109;
    public static final int SET_Veto_CRYPT_HPW = 110;
    public static final int SET_Veto_LOW_BAT_SEND = 111;
    //public static final int SET_Veto_CRYPT_NEW_SALT = 112;
    public static final int SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED = 113;
    public static final int SET_VetoSERVER_PUSH_URL = 114;
    public static final int SET_VetoSERVER_LAST_LOCATION_UPLOAD_TIME = 115;
    public static final int SET_VetoSERVER_LAST_CMD_MILLIS = 116;
    public static final int SET_Veto_EDGE_INFO_SHOWN = 117;

    public static final int SET_Veto_SERVER_CONNECTIVITY_CHECK_INTERVAL_HOURS = 201;
    public static final int SET_Veto_SERVER_CONNECTIVITY_CHECK_NOTIFY_AFTER_HOURS = 202;
    public static final int SET_Veto_SERVER_LAST_CONNECTIVITY_UNIX_TIME = 203;

    public static final int SET_FIRST_TIME_WHITELIST = 301;
    public static final int SET_FIRST_TIME_CONTACT_ADDED = 302;
    //public static final int SET_FIRST_TIME_Veto_SERVER = 303;

    public static final int SET_APP_CRASHED_LOG_ENTRY = 401; // 0=no crash, 1=crash to show
    public static final int SET_VetoSMS_COUNTER = 402;

    //public static final int SET_GPS_STATE = 501;         // 0=GPS is off 1=GPS is on 2=GPS is turned on by Veto
    public static final int SET_LAST_KNOWN_LOCATION_LAT = 502;
    public static final int SET_LAST_KNOWN_LOCATION_LON = 503;
    public static final int SET_LAST_KNOWN_LOCATION_TIME = 504;
    public static final int SET_LAST_LOW_BAT_UPLOAD = 505;
    public static final int SET_LAST_KNOWN_LOCATION_ACCURACY = 506;
    public static final int SET_LAST_KNOWN_LOCATION_ALTITUDE = 507;
    public static final int SET_LAST_KNOWN_LOCATION_BEARING = 508;
    public static final int SET_LAST_KNOWN_LOCATION_SPEED = 509;

    public static final int SET_THEME = 601;
    public static final String VAL_THEME_FOLLOW_SYSTEM = "follow_system";
    public static final String VAL_THEME_LIGHT = "light";
    public static final String VAL_THEME_DARK = "dark";
    public static final int SET_DYNAMIC_COLORS = 602;
    public static final int SET_CUSTOM_COLOR = 603;
    public static final int SET_THEFT_MODE_ACTIVE = 701;
    public static final int SET_THEFT_MODE_PIN = 702;

    public Settings() {
    }

    public Object get(int key) {
        if (super.containsKey(key)) {
            return super.get(key);
        } else {
            switch (key) {
                case SET_WIPE_ENABLED:
                case SET_ACCESS_VIA_PIN:
                case SET_FIRST_TIME_WHITELIST:
                case SET_FIRST_TIME_CONTACT_ADDED:
                    //case SET_FIRST_TIME_Veto_SERVER:
                case SET_VetoSERVER_PASSWORD_SET:
                    //case SET_Veto_CRYPT_NEW_SALT:
                case SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED:
                case SET_Veto_EDGE_INFO_SHOWN:
                case SET_THEFT_MODE_ACTIVE:
                    return false;
                case SET_THEFT_MODE_PIN:
                    return "";
                case SET_Veto_LOW_BAT_SEND:
                    return true;
                case SET_Veto_COMMAND:
                    return "veto";
                case SET_VetoSERVER_UPDATE_TIME:
                    return 60;
                //case SET_INTRODUCTION_VERSION:
                case SET_VetoSMS_COUNTER:
                case SET_SET_VERSION:
                    return 0;
                case SET_VetoSERVER_LOCATION_TYPE:
                    return BackgroundLocationType.Companion.getEMPTY();
                case SET_RINGER_TONE:
                    return RingerUtils.getDefaultRingtoneAsString();
                case SET_PIN:
                case SET_VetoSERVER_ID:
                case SET_LAST_KNOWN_LOCATION_LAT:
                case SET_LAST_KNOWN_LOCATION_LON:
                case SET_Veto_CRYPT_HPW:
                case SET_Veto_CRYPT_PRIVKEY:
                case SET_Veto_CRYPT_PUBKEY:
                case SET_VetoSERVER_URL:
                    return "";
                case SET_VetoSERVER_PUSH_URL:
                    return "";
                case SET_VetoSERVER_LAST_CMD_MILLIS:
                    return 0;
                //case SET_GPS_STATE:
                //    return 1;
                case SET_APP_CRASHED_LOG_ENTRY:
                    return 0;
                case SET_LAST_KNOWN_LOCATION_TIME:
                case SET_LAST_LOW_BAT_UPLOAD:
                case SET_VetoSERVER_LAST_LOCATION_UPLOAD_TIME:
                    return -1L;
                case SET_THEME:
                    return VAL_THEME_FOLLOW_SYSTEM;
                case SET_DYNAMIC_COLORS:
                    return false;
                case SET_CUSTOM_COLOR:
                    return 0xD4AF37; // Default Gold

                case SET_Veto_SERVER_CONNECTIVITY_CHECK_INTERVAL_HOURS:
                    return 0L;
                case SET_Veto_SERVER_CONNECTIVITY_CHECK_NOTIFY_AFTER_HOURS:
                    return 36L;
                case SET_Veto_SERVER_LAST_CONNECTIVITY_UNIX_TIME:
                    return 0L;

                case SET_LAST_KNOWN_LOCATION_ACCURACY,
                     SET_LAST_KNOWN_LOCATION_ALTITUDE,
                     SET_LAST_KNOWN_LOCATION_BEARING,
                     SET_LAST_KNOWN_LOCATION_SPEED:
                    return Float.NaN;
            }
        }
        return "";
    }

}
