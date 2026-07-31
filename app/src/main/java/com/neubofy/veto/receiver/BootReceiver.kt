package com.neubofy.veto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neubofy.veto.data.SettingsRepository

import com.neubofy.veto.services.TempContactExpiredService
import com.neubofy.veto.utils.log


class BootReceiver : BroadcastReceiver() {

    companion object {
        private val TAG: String = BootReceiver::class.java.simpleName

        const val BOOT_COMPLETED: String = "android.intent.action.BOOT_COMPLETED"
    }

    // Keep the BootReceiver so that the app launches once after boot.
    // However, the VetoApplication should start before this receiver runs, and it will start the main services.
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BOOT_COMPLETED) {
            context.log().i(TAG, "Running BOOT_COMPLETED handler")

            // One-shot services that don't need to run on every VetoApplication start
            TempContactExpiredService.scheduleJob(context, 0)

            val settings = SettingsRepository.getInstance(context)
            if (settings.get(com.neubofy.veto.data.Settings.SET_THEFT_MODE_ACTIVE) == true) {
                // To keep it simple, we just re-execute the ring command via its Activity
                // since the background service was removed.
                try {
                    com.neubofy.veto.ui.RingerActivity.newInstance(context, com.neubofy.veto.commands.RING_DURATION_LONG_SECS)
                } catch (e: Exception) {
                    context.log().e(TAG, "Failed to start RingerActivity on boot: \${e.message}")
                }
            }

        }
    }
}
