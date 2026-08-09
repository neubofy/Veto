package com.neubofy.veto.utils

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.services.AutoTheftDefenseService
import com.neubofy.veto.ui.AutoTheftWarningOverlay

object AutoTheftManager {
    private const val TAG = "AutoTheftManager"

    // BroadcastReceiver for ACTION_USER_PRESENT — sole mechanism to cancel warning on unlock
    private var unlockReceiver: BroadcastReceiver? = null

    fun triggerSuspectedMode(context: Context, reason: String) {
        val settings = SettingsRepository.getInstance(context)
        if (!(settings.get(Settings.SET_AUTO_THEFT_ENABLED) as Boolean)) return

        // If already in warning state, ignore re-trigger
        if (settings.get(Settings.SET_AUTO_THEFT_WARNING_ACTIVE) as Boolean) {
            context.log().w(TAG, "Already in warning state, ignoring re-trigger: $reason")
            return
        }

        context.log().w(TAG, "Triggering Auto Theft WARNING mode: $reason")
        settings.set(Settings.SET_AUTO_THEFT_WARNING_ACTIVE, true)
        AutoTheftDefenseManager.clearTerminal()

        // 1. Lock screen via Device Admin
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.lockNow()
        } catch (e: Exception) {
            context.log().e(TAG, "Failed to lock device: ${e.message}")
        }

        // 2. Launch custom Warning Overlay
        try {
            val lockMsg = settings.get(Settings.SET_AUTO_THEFT_LOCK_MSG) as? String ?: ""
            val intent = Intent(context, AutoTheftWarningOverlay::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(AutoTheftWarningOverlay.REASON_TEXT, reason)
                if (lockMsg.isNotEmpty()) {
                    putExtra(AutoTheftWarningOverlay.LOCK_MSG_TEXT, lockMsg)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            context.log().e(TAG, "Failed to launch AutoTheftWarningOverlay: ${e.message}")
        }

        // 3. Start Persistent AutoTheftDefenseService in Foreground
        try {
            AutoTheftDefenseService.startDefenseService(context.applicationContext)
        } catch (e: Exception) {
            context.log().e(TAG, "Failed to start AutoTheftDefenseService: ${e.message}")
        }

        // 4. Register ACTION_USER_PRESENT receiver — sole way to cancel via unlock
        registerUnlockReceiver(context.applicationContext)
    }

    fun cancelSuspectedMode(context: Context) {
        val settings = SettingsRepository.getInstance(context)
        if (!(settings.get(Settings.SET_AUTO_THEFT_WARNING_ACTIVE) as Boolean)) return

        context.log().i(TAG, "Cancelling Auto Theft WARNING mode (Device unlocked)")
        settings.set(Settings.SET_AUTO_THEFT_WARNING_ACTIVE, false)

        // 1. Stop Foreground Service
        try {
            AutoTheftDefenseService.stopDefenseService(context.applicationContext)
        } catch (e: Exception) {
            context.log().e(TAG, "Failed to stop AutoTheftDefenseService: ${e.message}")
        }

        // 2. Stop Ringing & TTS
        com.neubofy.veto.services.RingerService.stopRinging(context)
        AutoTheftDefenseManager.stopTts()

        // 3. Unregister unlock receiver
        unregisterUnlockReceiver(context.applicationContext)
    }

    private fun registerUnlockReceiver(appContext: Context) {
        if (unlockReceiver != null) return

        unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_USER_PRESENT) {
                    appContext.log().i(TAG, "ACTION_USER_PRESENT received — cancelling auto theft warning")
                    cancelSuspectedMode(appContext)
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        appContext.registerReceiver(unlockReceiver, filter)
        appContext.log().d(TAG, "Registered ACTION_USER_PRESENT receiver for unlock proof")
    }

    private fun unregisterUnlockReceiver(appContext: Context) {
        unlockReceiver?.let {
            try {
                appContext.unregisterReceiver(it)
            } catch (e: Exception) {
                appContext.log().w(TAG, "Failed to unregister unlock receiver: ${e.message}")
            }
            unlockReceiver = null
        }
    }
}
