package com.neubofy.veto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.services.TempContactExpiredService
import com.neubofy.veto.utils.log


class AppUpdatedReceiver : BroadcastReceiver() {

    companion object {
        private val TAG: String = AppUpdatedReceiver::class.java.simpleName

        const val APP_UPDATED: String = "android.intent.action.MY_PACKAGE_REPLACED"
    }

    // Keep the AppUpdatedReceiver so that the app launches once after app updates.
    // However, the VetoApplication should start before this receiver runs, and it will start the main services.
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == APP_UPDATED) {
            context.log().i(TAG, "Running MY_PACKAGE_REPLACED (APP_UPDATED) handler")

            // One-shot services that don't need to run on every VetoApplication start
            TempContactExpiredService.scheduleJob(context, 0)
            
            // Sync Firebase Token to Dashboard on App Update
            com.neubofy.veto.utils.DashboardSync.uploadTokenIfPaired(context)

        }
    }
}
