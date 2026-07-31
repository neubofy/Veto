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


class PostNotificationsPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_post_notification_name

    @get:StringRes
    override val description = R.string.Permission_POST_NOTIFICATIONS

    override fun isGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PERMISSION_GRANTED
        } else {
            true
        }
    }

    val REQUEST_CODE = 42034

    override fun request(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE
            )
        }
    }
}
