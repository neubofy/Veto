package com.neubofy.veto.utils

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.AutoTheftWarningOverlay
import com.neubofy.veto.transports.NextJsServerTransport
import kotlinx.coroutines.*
import java.util.Locale

object AutoTheftManager : TextToSpeech.OnInitListener {
    private const val TAG = "AutoTheftManager"
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingMessage: String? = null
    private var vibrationJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

        // 2. Launch custom Warning Overlay
        try {
            val intent = Intent(context, AutoTheftWarningOverlay::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(AutoTheftWarningOverlay.REASON_TEXT, reason)
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
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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

        // 5. Build TTS message and speak exactly 3 times
        val proofs = mutableListOf<String>()
        if (settings.get(Settings.SET_AUTO_THEFT_PROOF_UNLOCK) as Boolean) proofs.add("unlock device")
        if (settings.get(Settings.SET_AUTO_THEFT_PROOF_CHARGE) as Boolean) proofs.add("plug into charger")
        if (settings.get(Settings.SET_AUTO_THEFT_PROOF_SIM) as Boolean) proofs.add("reinsert owner SIM")

        val proofMsg = if (proofs.isNotEmpty()) " To verify ownership, please ${proofs.joinToString(" or ")}." else ""
        val ttsMsg = "Theft suspected: $reason.$proofMsg"
        speakWarning(context.applicationContext, ttsMsg)

        // 6. Send Warning info to server
        scope.launch {
            try {
                val transport = NextJsServerTransport(context)
                transport.send(context, "⚠️ Theft Warning Triggered: $reason. Awaiting owner verification.", "theft_warning")
            } catch (e: Exception) {
                context.log().e(TAG, "Failed to notify server: ${e.message}")
            }
        }
    }

    fun cancelSuspectedMode(context: Context) {
        val settings = SettingsRepository.getInstance(context)
        if (!(settings.get(Settings.SET_AUTO_THEFT_WARNING_ACTIVE) as Boolean)) return

        context.log().i(TAG, "Cancelling Auto Theft WARNING mode (Legitimacy proven)")
        settings.set(Settings.SET_AUTO_THEFT_WARNING_ACTIVE, false)

        // 1. Stop vibration
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
            vibrationJob?.cancel()
        } catch (e: Exception) {
            context.log().e(TAG, "Failed to cancel vibration: ${e.message}")
        }

        // 2. Stop TTS
        tts?.stop()

        // 3. Notify Server
        scope.launch {
            try {
                val transport = NextJsServerTransport(context)
                transport.send(context, "✅ Legitimate User Proven. Theft warning cancelled.", "theft_warning_cancelled")
            } catch (e: Exception) {
                context.log().e(TAG, "Failed to notify server of cancellation: ${e.message}")
            }
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
