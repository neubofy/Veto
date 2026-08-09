package com.neubofy.veto.utils

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.AutoTheftWarningOverlay
import kotlinx.coroutines.*
import java.util.Locale

object AutoTheftManager : TextToSpeech.OnInitListener {
    private const val TAG = "AutoTheftManager"
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingMessage: String? = null
    private var vibrationJob: Job? = null

    // BroadcastReceiver for ACTION_USER_PRESENT — sole mechanism to cancel warning on unlock
    private var unlockReceiver: BroadcastReceiver? = null

    fun triggerSuspectedMode(context: Context, reason: String) {
        val settings = SettingsRepository.getInstance(context)
        if (!(settings.get(Settings.SET_AUTO_THEFT_ENABLED) as Boolean)) return

        // If already in warning state, ignore re-trigger (escalation handles failed logins separately)
        if (settings.get(Settings.SET_AUTO_THEFT_WARNING_ACTIVE) as Boolean) {
            context.log().w(TAG, "Already in warning state, ignoring re-trigger: $reason")
            return
        }

        context.log().w(TAG, "Triggering Auto Theft WARNING mode: $reason")
        settings.set(Settings.SET_AUTO_THEFT_WARNING_ACTIVE, true)

        // 1. Lock screen via Device Admin
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            dpm.lockNow()
        } catch (e: Exception) {
            context.log().e(TAG, "Failed to lock device: ${e.message}")
        }

        // 2. Launch custom Warning Overlay with lock message
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

        // 3. Maximize Volume (Music stream for TTS)
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        } catch (e: Exception) {
            context.log().e(TAG, "Failed to set volume: ${e.message}")
        }

        // 4. Vibrate for strict 30s limit
        try {
            val vibrator = getVibrator(context)
            if (vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 1000, 1000)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                vibrationJob?.cancel()
                vibrationJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(30000)
                    vibrator.cancel()
                }
            }
        } catch (e: Exception) {
            context.log().e(TAG, "Failed to vibrate: ${e.message}")
        }

        // 5. Build TTS message — only unlock proof
        val ttsMsg = "Theft suspected: $reason. To verify ownership, please unlock the device."
        speakWarning(context.applicationContext, ttsMsg)

        // 6. Register ACTION_USER_PRESENT receiver — sole way to cancel via unlock
        registerUnlockReceiver(context.applicationContext)
    }

    fun cancelSuspectedMode(context: Context) {
        val settings = SettingsRepository.getInstance(context)
        if (!(settings.get(Settings.SET_AUTO_THEFT_WARNING_ACTIVE) as Boolean)) return

        context.log().i(TAG, "Cancelling Auto Theft WARNING mode (Device unlocked)")
        settings.set(Settings.SET_AUTO_THEFT_WARNING_ACTIVE, false)

        // 1. Stop vibration
        try {
            val vibrator = getVibrator(context)
            vibrator.cancel()
            vibrationJob?.cancel()
        } catch (e: Exception) {
            context.log().e(TAG, "Failed to cancel vibration: ${e.message}")
        }

        // 2. Stop TTS
        tts?.stop()

        // 3. Unregister unlock receiver
        unregisterUnlockReceiver(context.applicationContext)
    }

    private fun registerUnlockReceiver(appContext: Context) {
        if (unlockReceiver != null) return // Already registered

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

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun speakWarning(context: Context, message: String) {
        pendingMessage = message
        if (tts == null) {
            tts = TextToSpeech(context, this)
        } else if (isTtsInitialized) {
            speakRepeatedly(message)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsInitialized = true
            pendingMessage?.let {
                speakRepeatedly(it)
                pendingMessage = null
            }
        }
    }

    private fun speakRepeatedly(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "WarningTTS")
        tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "WarningTTS")
        tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "WarningTTS")
    }
}
