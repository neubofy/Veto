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
        val isConfirmed = settings.get(Settings.SET_THEFT_MODE_CONFIRMED) as Boolean
        
        if (isTheftActive && isConfirmed) {
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
                settings.set(Settings.SET_THEFT_WRONG_PASS_COUNT, 0)
                
                if (isTheftActive) {
                    // Already triggered, so this event confirms theft
                    settings.set(Settings.SET_THEFT_MODE_CONFIRMED, true)
                    val badEventIntent = Intent(context, TheftModeService::class.java).apply {
                        action = "ACTION_BAD_EVENT_WRONG_PASS_LIMIT"
                    }
                    androidx.core.content.ContextCompat.startForegroundService(context, badEventIntent)
                } else {
                    // Trigger theft mode normally
                    TheftCommand(context).executeInternal(context)
                }
            } else {
                settings.set(Settings.SET_THEFT_WRONG_PASS_COUNT, currentCount)
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        val settings = SettingsRepository.getInstance(context)
        
        // Only write to database if the count is greater than 0
        // This avoids writing to disk 200-500 times a day on normal unlocks
        val currentCount = settings.get(Settings.SET_THEFT_WRONG_PASS_COUNT) as Int
        if (currentCount > 0) {
            settings.set(Settings.SET_THEFT_WRONG_PASS_COUNT, 0)
        }
    }
}
