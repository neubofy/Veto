package com.neubofy.veto.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.neubofy.veto.R


class CameraPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_camera_name
    @get:StringRes
    override val description = R.string.perm_camera_desc

    override fun isGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PERMISSION_GRANTED
    }

    val REQUEST_CODE = 8020

    override fun request(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(Manifest.permission.CAMERA), REQUEST_CODE
        )
    }
}
