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

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BOOT_COMPLETED) {
            context.log().i(TAG, "Running BOOT_COMPLETED handler")

            // One-shot services that don't need to run on every VetoApplication start
            TempContactExpiredService.scheduleJob(context, 0)

            val settings = SettingsRepository.getInstance(context)
            if (settings.get(com.neubofy.veto.data.Settings.SET_THEFT_MODE_ACTIVE) == true) {
                try {
                    context.log().i(TAG, "Theft mode was active before boot, restarting as confirmed theft.")
                    val theftCommand = com.neubofy.veto.commands.TheftCommand(context)
                    theftCommand.executeInternal(context)
                } catch (e: Exception) {
                    context.log().e(TAG, "Failed to restart theft recovery on boot: ${e.message}")
                }
            }
        }
    }
}
