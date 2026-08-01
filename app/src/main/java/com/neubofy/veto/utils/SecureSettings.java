package com.neubofy.veto.utils;

import android.content.Context;
import android.provider.Settings;

public class SecureSettings {

    private static final String TAG = SecureSettings.class.getSimpleName();

    public static void turnGPS(Context context, boolean enable) {
        int value;
        if (enable) {
            value = android.provider.Settings.Secure.LOCATION_MODE_HIGH_ACCURACY;
        } else {
            value = android.provider.Settings.Secure.LOCATION_MODE_OFF;
        }
        Settings.Secure.putString(context.getContentResolver(), android.provider.Settings.Secure.LOCATION_MODE, Integer.valueOf(value).toString());
        VetoLogKt.log(context).d(TAG, "Turned GPS on/off using SecureSettings: " + enable);
    }

    public static boolean setBluetooth(Context context, boolean enable) {
        try {
            int value = enable ? 1 : 0;
            Settings.Global.putInt(context.getContentResolver(), "bluetooth_on", value);
            VetoLogKt.log(context).d(TAG, "Turned Bluetooth on/off using SecureSettings: " + enable);
            return true;
        } catch (Exception e) {
            VetoLogKt.log(context).e(TAG, "Failed to toggle bluetooth via SecureSettings: " + e.getMessage());
            return false;
        }
    }
}
