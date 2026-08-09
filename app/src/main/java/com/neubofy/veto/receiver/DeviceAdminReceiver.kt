package com.neubofy.veto.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.utils.AutoTheftManager

class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        val settings = SettingsRepository.getInstance(context)

        if (settings.get(Settings.SET_AUTO_THEFT_FAILED_UNLOCK) as Boolean) {
            val failedAttempts = intent.getIntExtra("android.app.extra.FAILED_PASSWORD_ATTEMPTS", 0)
            val maxAttempts = settings.get(Settings.SET_AUTO_THEFT_MAX_ATTEMPTS) as Int

            if (failedAttempts >= maxAttempts) {
                AutoTheftManager.triggerSuspectedMode(context, "Too many failed unlock attempts")
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        val settings = SettingsRepository.getInstance(context)

        if (settings.get(Settings.SET_AUTO_THEFT_PROOF_UNLOCK) as Boolean) {
            AutoTheftManager.cancelSuspectedMode(context)
        }
    }
}
