package com.neubofy.veto.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.UserHandle
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neubofy.veto.R


class WriteSecureSettingsPermission : Permission() {

    companion object {
        private val TAG = WriteSecureSettingsPermission::class.simpleName
    }

    @get:StringRes
    override val name = R.string.perm_write_secure_settings_name

    override fun isGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PERMISSION_GRANTED
    }

    override fun request(activity: Activity) {
        val userId = getUserId(activity)
        MaterialAlertDialogBuilder(activity).apply {
            setTitle(R.string.grant_write_secure_settings_title)
            setMessage(activity.getString(R.string.grant_write_secure_settings_description, userId))
            setPositiveButton(R.string.grant_via_adb) { _, _ -> requestManually(activity) }
            setNegativeButton(R.string.cancel, null)
        }.show()
    }

    private fun requestManually(activity: Activity) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            "https://veto-foss.org/docs/veto-android/granting-secure-settings-access".toUri()
        )
        activity.startActivity(intent)
    }

    private fun getUserId(activity: Activity): Int {
        return UserHandle.getUserHandleForUid(activity.taskId).describeContents()
    }
}