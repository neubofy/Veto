package com.neubofy.veto.permissions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.StringRes
import com.neubofy.veto.R


class BatteryOptimizationsPermission() : Permission() {
    @get:StringRes
    override val name = R.string.perm_battery_optimizations_name

    @get:StringRes
    override val description = R.string.Permission_IGNORE_BATTERY_OPTIMIZATION

    override fun isGranted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    @SuppressLint("BatteryLife")
    override fun request(activity: Activity) {
        val packageName = activity.packageName
        val intent = Intent().apply {
            setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            setData(Uri.parse("package:$packageName"))
        }
        activity.startActivity(intent)
    }
}
