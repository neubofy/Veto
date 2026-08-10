package com.neubofy.veto.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.neubofy.veto.commands.TheftCommand
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.services.TheftModeService

class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        val settings = SettingsRepository.getInstance(context)
        
        val isTheftActive = settings.get(Settings.SET_THEFT_MODE_ACTIVE) as Boolean
        if (isTheftActive) {
            // Already active, just broadcast bad event
            val broadcastIntent = Intent(context, TheftModeService::class.java).apply {
                action = TheftModeService.ACTION_GOOD_EVENT // Actually wait, there is no ACTION_BAD_EVENT handled via intent. 
                // The service listens to TheftSensorManager. But we can just use intent to trigger it or we can bind.
                // Let's send a broadcast and the service can register a receiver.
            }
            // Better yet, just start the service with a specific action
            val badEventIntent = Intent(context, TheftModeService::class.java).apply {
                action = "ACTION_BAD_EVENT_WRONG_PASS"
            }
            androidx.core.content.ContextCompat.startForegroundService(context, badEventIntent)
            return
        }

        val enabled = settings.get(Settings.SET_THEFT_WRONG_PASS_ENABLED) as Boolean
        if (enabled) {
            val limit = settings.get(Settings.SET_THEFT_WRONG_PASS_ATTEMPTS) as Int
            var currentCount = settings.get(Settings.SET_THEFT_WRONG_PASS_COUNT) as Int
            currentCount++
            
            if (currentCount >= limit) {
                // Trigger theft mode
                settings.set(Settings.SET_THEFT_WRONG_PASS_COUNT, 0)
                TheftCommand(context).executeInternal(context)
            } else {
                settings.set(Settings.SET_THEFT_WRONG_PASS_COUNT, currentCount)
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        val settings = SettingsRepository.getInstance(context)
        settings.set(Settings.SET_THEFT_WRONG_PASS_COUNT, 0)
    }
}
