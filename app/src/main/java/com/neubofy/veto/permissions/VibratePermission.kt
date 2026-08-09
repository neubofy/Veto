package com.neubofy.veto.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.neubofy.veto.R

class VibratePermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_vibrate_name

    @get:StringRes
    override val description = R.string.perm_vibrate_desc

    val REQUEST_CODE = 8056

    override fun isGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.VIBRATE
        ) == PERMISSION_GRANTED
    }

    override fun request(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.VIBRATE),
            REQUEST_CODE
        )
    }
}
