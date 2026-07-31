package com.neubofy.veto.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.neubofy.veto.R


class SmsPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_sms_name
    @get:StringRes
    override val description = R.string.perm_sms_desc

    val REQUEST_CODE = 8070

    override fun isGranted(context: Context): Boolean {
        return (
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.SEND_SMS
                ) == PERMISSION_GRANTED)
                && (
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECEIVE_SMS
                ) == PERMISSION_GRANTED)
                && (
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_SMS
                ) == PERMISSION_GRANTED)
    }

    override fun request(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            ),
            REQUEST_CODE
        )
    }
}
