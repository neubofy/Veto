package com.neubofy.veto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository

class SimStateReceiver : BroadcastReceiver() {

    companion object {
        private var lastTriggerTime: Long = 0
        private const val DEBOUNCE_MS = 5000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "android.intent.action.SIM_STATE_CHANGED") {
            val state = intent.getStringExtra("ss") ?: return
            
            // Log.d("SimStateReceiver", "SIM state changed: $state")

            val now = System.currentTimeMillis()
            if (now - lastTriggerTime < DEBOUNCE_MS) {
                return
            }

            val settings = SettingsRepository.getInstance(context)
            settings.load()

            val isTheftActive = settings.get(Settings.SET_THEFT_MODE_ACTIVE) as Boolean

            when (state) {
                "ABSENT" -> {
                    if (!isTheftActive) {
                        val autoDetect = settings.get(Settings.SET_THEFT_AUTO_DETECT_ENABLED) as Boolean
                        if (autoDetect) {
                            lastTriggerTime = now
                            triggerTheftMode(context)
                        }
                    }
                }
                "LOADED", "READY" -> {
                    if (isTheftActive) {
                        // SIM inserted while theft mode is active - might be the owner.
                        // Send a good event to the service to stop disturb if it's running.
                        lastTriggerTime = now
                        val serviceIntent = Intent(context, com.neubofy.veto.services.TheftModeService::class.java).apply {
                            this.action = "ACTION_GOOD_EVENT"
                            putExtra("type", "sim_inserted")
                        }
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }

    private fun triggerTheftMode(context: Context) {
        // Execute the theft command internally
        val theftCommand = com.neubofy.veto.commands.TheftCommand(context)
        theftCommand.executeInternal(context)
    }
}
