package com.neubofy.veto.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.neubofy.veto.R


class LocationPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_location_name
    @get:StringRes
    override val description = R.string.perm_location_desc

    fun isForegroundGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PERMISSION_GRANTED
    }

    fun isBackgroundGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PERMISSION_GRANTED
        } else true
    }

    override fun isGranted(context: Context): Boolean {
        return isForegroundGranted(context) && isBackgroundGranted(context)
    }

    val REQUEST_CODE = 8050

    override fun request(activity: Activity) {
        // We cannot request ACCESS_FINE_LOCATION and ACCESS_BACKGROUND_LOCATION at the same time
        // The must be granted one after the other.
        // Thus this method will usually be called twice (since calling it once will not get you both permissions).
        if (!isForegroundGranted(activity.applicationContext)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE
            )
            return
        }
        if (!isBackgroundGranted(activity.applicationContext)
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_CODE
            )
        }
    }
}
