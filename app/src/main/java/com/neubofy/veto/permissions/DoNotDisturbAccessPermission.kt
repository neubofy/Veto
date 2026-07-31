package com.neubofy.veto.permissions

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.StringRes
import com.neubofy.veto.R


class DoNotDisturbAccessPermission() : Permission() {
    @get:StringRes
    override val name = R.string.perm_do_not_disturb_access_name
    @get:StringRes
    override val description = R.string.perm_dnd_desc

    override fun isGranted(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    override fun request(activity: Activity) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        activity.startActivity(intent)
    }
}
