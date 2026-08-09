package com.neubofy.veto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives power connection events.
 * Auto-theft charge proof has been removed — only device unlock cancels theft warnings.
 */
class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op: Charge-based theft proof removed.
        // Auto-theft cancellation is now handled exclusively via device unlock (ACTION_USER_PRESENT).
    }
}
