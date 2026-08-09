package com.neubofy.veto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.utils.AutoTheftManager

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_POWER_CONNECTED) {
            val settings = SettingsRepository.getInstance(context)
            if (settings.get(Settings.SET_AUTO_THEFT_PROOF_CHARGE) as Boolean) {
                AutoTheftManager.cancelSuspectedMode(context)
            }
        }
    }
}
