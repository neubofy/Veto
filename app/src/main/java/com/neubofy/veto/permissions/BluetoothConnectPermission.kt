package com.neubofy.veto.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.neubofy.veto.R


@RequiresApi(Build.VERSION_CODES.S)
class BluetoothConnectPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_bluetooth_connect_name
    @get:StringRes
    override val description = R.string.perm_bluetooth_connect_desc

    override fun isGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PERMISSION_GRANTED
    }

    val REQUEST_CODE = 8095

    override fun request(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_CODE
        )
    }
}
