package com.neubofy.veto.permissions

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.concurrent.futures.await
import androidx.core.content.IntentCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants
import com.neubofy.veto.R
import kotlinx.coroutines.runBlocking


class UnusedAppRestrictionsPermission() : Permission() {
    @get:StringRes
    override val name = R.string.perm_unused_app_name

    @get:StringRes
    override val description = R.string.perm_unused_app_description

    override fun isGranted(context: Context): Boolean {
        // TODO: properly do coroutines
        return runBlocking { isGrantedSuspend(context) }
    }

    private suspend fun isGrantedSuspend(context: Context): Boolean {
        // If an app is Device Admin, the app is automatically exempted.
        // Also the toggle is disabled/greyed out, so the user cannot even change it.
        val isAdmin = DeviceAdminPermission().isGranted(context)
        if (isAdmin) {
            return true
        }

        val future = PackageManagerCompat.getUnusedAppRestrictionsStatus(context)
        val appRestrictionsStatus = future.await()
        return when (appRestrictionsStatus) {
            UnusedAppRestrictionsConstants.DISABLED, UnusedAppRestrictionsConstants.FEATURE_NOT_AVAILABLE -> true
            else -> false
        }
    }

    override fun request(activity: Activity) {
        val packageName = activity.packageName
        val intent = IntentCompat.createManageUnusedAppRestrictionsIntent(activity, packageName)
        // must use startActivityForResult() instead of startActivity() according to the docs
        activity.startActivityForResult(intent, 42)
    }
}
