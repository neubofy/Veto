package com.neubofy.veto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.utils.AutoTheftManager
import com.neubofy.veto.utils.Notifications
import com.neubofy.veto.utils.log

/**
 * Monitors SIM state changes. Only triggers auto-theft warning on SIM removal.
 * Cancellation is handled exclusively by device unlock (ACTION_USER_PRESENT) in AutoTheftManager.
 */
class SimStateReceiver : BroadcastReceiver() {
    companion object {
        private val TAG = SimStateReceiver::class.simpleName
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.SIM_STATE_CHANGED") return

        val state = intent.getStringExtra("ss") ?: return
        context.log().d(TAG, "SIM state changed: $state")

        val settings = SettingsRepository.getInstance(context)
        val isEnabled = settings.get(Settings.SET_AUTO_THEFT_ENABLED) as Boolean
        val triggerOnSimRemoved = settings.get(Settings.SET_AUTO_THEFT_SIM_REMOVED) as Boolean

        if (!isEnabled || !triggerOnSimRemoved) return

        // Only trigger on SIM removal — cancellation is handled by device unlock only
        val isAbsent = state == "ABSENT"
        if (isAbsent) {
            context.log().w(TAG, "SIM card removed! Triggering auto theft warning.")
            Notifications.notify(
                context,
                "⚠️ SIM Card Removed",
                "SIM card was extracted. Auto-theft protection activated. Unlock device to dismiss.",
                Notifications.CHANNEL_FAILED
            )
            AutoTheftManager.triggerSuspectedMode(context, "SIM card removed")
        }
    }
}
