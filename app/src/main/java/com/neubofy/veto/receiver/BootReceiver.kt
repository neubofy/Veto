package com.neubofy.veto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.services.TempContactExpiredService
import com.neubofy.veto.ui.AutoTheftWarningOverlay
import com.neubofy.veto.utils.AutoTheftManager
import com.neubofy.veto.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BootReceiver : BroadcastReceiver() {

    companion object {
        private val TAG: String = BootReceiver::class.java.simpleName
        const val BOOT_COMPLETED: String = "android.intent.action.BOOT_COMPLETED"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BOOT_COMPLETED) {
            context.log().i(TAG, "Running BOOT_COMPLETED handler")

            TempContactExpiredService.scheduleJob(context, 0)

            val settings = SettingsRepository.getInstance(context)

            // Auto-Theft Persistence: If device was restarted while auto-theft is active, re-trigger defense overlay!
            val isAutoTheftActive = settings.get(Settings.SET_AUTO_THEFT_WARNING_ACTIVE) as? Boolean ?: false
            if (isAutoTheftActive) {
                context.log().w(TAG, "Auto-Theft active on boot! Re-launching defense overlay & service.")
                AutoTheftManager.triggerSuspectedMode(context, "Device rebooted during auto-theft protection")
            }

            if (settings.get(Settings.SET_THEFT_MODE_ACTIVE) == true) {
                try {
                    val dummyTransport = com.neubofy.veto.transports.InAppTransport(context)
                    val lockCommand = com.neubofy.veto.commands.LockCommand(context)
                    CoroutineScope(Dispatchers.IO).launch {
                        lockCommand.execute(emptyList(), dummyTransport)
                    }
                    com.neubofy.veto.services.RingerService.startRinging(context, com.neubofy.veto.commands.RING_DURATION_DEFAULT_SECS)
                } catch (e: Exception) {
                    context.log().e(TAG, "Failed to start theft recovery on boot: ${e.message}")
                }
            }
        }
    }
}
