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

class PhoneStatePermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_phone_state_name

    @get:StringRes
    override val description = R.string.perm_phone_state_desc

    val REQUEST_CODE = 8055

    override fun isGranted(context: Context): Boolean {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PERMISSION_GRANTED

        val hasPhoneNumbers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_NUMBERS
            ) == PERMISSION_GRANTED
        } else true

        return hasPhoneState && hasPhoneNumbers
    }

    override fun request(activity: Activity) {
        val perms = mutableListOf(Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            perms.add(Manifest.permission.READ_PHONE_NUMBERS)
        }
        ActivityCompat.requestPermissions(
            activity,
            perms.toTypedArray(),
            REQUEST_CODE
        )
    }
}
