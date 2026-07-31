package com.neubofy.veto.permissions

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.services.NotificationListenService


class NotificationAccessPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_notification_access_name
    @get:StringRes
    override val description = R.string.perm_notification_access_desc

    override fun isGranted(context: Context): Boolean {
        val cn = ComponentName(context, NotificationListenService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return nm.isNotificationListenerAccessGranted(cn)
        } else {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners" // Settings.Secure.ENABLED_NOTIFICATION_LISTENERS
            )
            return flat != null && flat.contains(cn.flattenToString())
        }
    }

    override fun request(activity: Activity) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        activity.startActivity(intent)
    }
}
