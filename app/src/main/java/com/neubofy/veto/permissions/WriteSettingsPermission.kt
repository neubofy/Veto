package com.neubofy.veto.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import com.neubofy.veto.R

class WriteSettingsPermission : Permission() {
    @get:StringRes
    override val name = R.string.perm_write_settings_name
    @get:StringRes
    override val description = R.string.perm_write_settings_desc

    override fun isGranted(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    override fun request(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:" + activity.packageName)
        )
        activity.startActivity(intent)
    }
}
