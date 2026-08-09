package com.neubofy.veto.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.neubofy.veto.commands.TheftCommand
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.transports.InAppTransport
import com.neubofy.veto.utils.AutoTheftManager
import com.neubofy.veto.utils.Notifications
import com.neubofy.veto.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceAdminReceiver : DeviceAdminReceiver() {

    @Deprecated("Deprecated in Java")
    override fun onPasswordFailed(context: Context, intent: Intent) {
        @Suppress("DEPRECATION")
        super.onPasswordFailed(context, intent)
        context.log().w("DeviceAdminReceiver", "Password attempt failed on device!")

        val settings = SettingsRepository.getInstance(context)
        val autoTheftEnabled = settings.get(Settings.SET_AUTO_THEFT_ENABLED) as? Boolean ?: false
        val failedUnlockEnabled = settings.get(Settings.SET_AUTO_THEFT_FAILED_UNLOCK) as? Boolean ?: false

        // Master toggle must be on
        if (!autoTheftEnabled) return

        context.log().w("DeviceAdminReceiver", "ACTION_PASSWORD_FAILED triggered.")

        // 1. Calculate failed attempts & remaining attempts
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
        val dpmAttempts = dpm?.currentFailedPasswordAttempts ?: 0

        val currentLocalCount = (settings.get(Settings.SET_AUTO_THEFT_FAILED_COUNTER) as? Int ?: 0) + 1
        settings.set(Settings.SET_AUTO_THEFT_FAILED_COUNTER, currentLocalCount)

        val failedAttempts = maxOf(dpmAttempts, currentLocalCount)
        val maxAttempts = (settings.get(Settings.SET_AUTO_THEFT_MAX_ATTEMPTS) as? Int) ?: 3
        val remainingAttempts = (maxAttempts - failedAttempts).coerceAtLeast(0)

        // 2. Check if device is ALREADY in Warning mode -> Escalate to full Theft Mode!
        if (settings.get(Settings.SET_AUTO_THEFT_WARNING_ACTIVE) as Boolean) {
            context.log().w("DeviceAdminReceiver", "Failed unlock attempt while in Warning mode! Escalating to full Theft Mode!")

            // Send warning escalation notification
            Notifications.notify(
                context = context,
                title = "🚨 Theft Confirmed!",
                text = "Subsequent failed unlock attempt detected during Warning state. Activating full alarm siren & tracking!",
                channelID = Notifications.CHANNEL_FAILED
            )

            AutoTheftManager.cancelSuspectedMode(context)
            val theftCommand = TheftCommand(context)
            CoroutineScope(Dispatchers.IO).launch {
                theftCommand.executeInternal(emptyList(), InAppTransport(context))
            }
            return
        }

        // 3. Send Notification on EVERY wrong password attempt to inform the user
        val notifTitle = "⚠️ Invalid Unlock Attempt ($failedAttempts/$maxAttempts)"
        val notifBody = if (remainingAttempts > 0) {
            "Incorrect password/PIN entered. $remainingAttempts attempt(s) left before auto-theft warning triggers and device locks."
        } else {
            "Max failed unlock attempts ($maxAttempts) reached! Triggering Auto-Theft Warning mode."
        }

        Notifications.notify(
            context = context,
            title = notifTitle,
            text = notifBody,
            channelID = Notifications.CHANNEL_FAILED
        )

        // 4. Trigger Auto-Theft Warning Mode if threshold reached
        if (failedUnlockEnabled && failedAttempts >= maxAttempts) {
            context.log().w("DeviceAdminReceiver", "Max failed unlock attempts ($failedAttempts >= $maxAttempts) reached! Triggering Auto-Theft Warning mode.")
            AutoTheftManager.triggerSuspectedMode(context, "$failedAttempts failed unlock attempts")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        @Suppress("DEPRECATION")
        super.onPasswordSucceeded(context, intent)
        context.log().i("DeviceAdminReceiver", "Password succeeded on device.")
        val settings = SettingsRepository.getInstance(context)
        settings.set(Settings.SET_AUTO_THEFT_FAILED_COUNTER, 0)

        // Device unlock is the sole way to cancel auto theft warning
        AutoTheftManager.cancelSuspectedMode(context)
    }
}
