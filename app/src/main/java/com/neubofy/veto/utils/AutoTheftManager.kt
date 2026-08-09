package com.neubofy.veto.utils

import android.content.Context
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import com.neubofy.veto.commands.LockCommand
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.transports.InAppTransport
import kotlinx.coroutines.*
import java.util.Locale

object AutoTheftManager : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingMessage: String? = null
    private var vibrationJob: Job? = null

    fun triggerSuspectedMode(context: Context, reason: String) {
        val settings = SettingsRepository.getInstance(context)
        if (!(settings.get(Settings.SET_AUTO_THEFT_ENABLED) as Boolean)) return

        settings.set(Settings.SET_THEFT_MODE_ACTIVE, true)

        // 1. Lock phone with custom message
        val lockMsg = settings.get(Settings.SET_AUTO_THEFT_LOCK_MSG) as String
        val lockArgs = if (lockMsg.isNotEmpty()) listOf("msg", lockMsg) else emptyList()
        val dummyTransport = InAppTransport(context)
        val lockCommand = LockCommand(context)
        CoroutineScope(Dispatchers.IO).launch {
            lockCommand.execute(lockArgs, dummyTransport)
        }

        // 2. Maximize volume
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

        // 3. Vibrate for strict 30s limit
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

        // 4. Build TTS message and speak 3 times
        val proofs = mutableListOf<String>()
        if (settings.get(Settings.SET_AUTO_THEFT_PROOF_UNLOCK) as Boolean) proofs.add("unlock device")
        if (settings.get(Settings.SET_AUTO_THEFT_PROOF_CHARGE) as Boolean) proofs.add("plug into charger")
        if (settings.get(Settings.SET_AUTO_THEFT_PROOF_SIM) as Boolean) proofs.add("reinsert owner SIM")

        val proofMsg = if (proofs.isNotEmpty()) " To verify ownership, please ${proofs.joinToString(" or ")}." else ""
        val ttsMsg = "Theft suspected: $reason.$proofMsg"

        speakWarning(context, ttsMsg)
    }

    fun cancelSuspectedMode(context: Context) {
        val settings = SettingsRepository.getInstance(context)
        if (settings.get(Settings.SET_THEFT_MODE_ACTIVE) as Boolean) {
            settings.set(Settings.SET_THEFT_MODE_ACTIVE, false)

            // Stop vibration
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.cancel()
            vibrationJob?.cancel()

            // Stop TTS
            tts?.stop()
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
        // Speak exactly 3 times
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "WarningTTS")
        tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "WarningTTS")
        tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "WarningTTS")
    }
}
