package com.neubofy.veto.permissions

import android.content.Context

// Order matters for the permissions screen
fun globalAppPermissions() = listOf(
    PostNotificationsPermission(),
    BatteryOptimizationsPermission(),
    UnusedAppRestrictionsPermission(),
    BluetoothConnectPermission(),
    CameraPermission(),
    DeviceAdminPermission(),
    DoNotDisturbAccessPermission(),
    LocationPermission(),
    NotificationAccessPermission(),
    OverlayPermission(),
    SmsPermission(),
    WriteSecureSettingsPermission()
)

fun isMissingGlobalAppPermission(context: Context): Boolean {
    return globalAppPermissions().any { perm -> !perm.isGranted(context) }
}
