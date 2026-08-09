package com.neubofy.veto.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.commands.TheftCommand
import com.neubofy.veto.transports.InAppTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class DeviceAdminReceiver : DeviceAdminReceiver(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingMessage: String? = null

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        val settings = SettingsRepository.getInstance(context)
        val failedAttempts = intent.getIntExtra("android.app.extra.FAILED_PASSWORD_ATTEMPTS", 0)

        // Ensure theft mode is active or triggered
        if (failedAttempts == 1) {
            // First failure, speak a warning and lock/vibrate (handled in onReceive normally, but we can do it here)
            speakWarning(context, "Theft suspected. Please unlock to verify you are the owner.")
            // Vibration and lock will happen automatically if force-lock policy is used? No, we should probably trigger lock
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            dpm.lockNow()
        } else if (failedAttempts > 1) {
            // Trigger theft command
            settings.set(Settings.SET_THEFT_MODE_ACTIVE, true)
            val dummyTransport = InAppTransport(context)
            val theftCommand = TheftCommand(context)
            CoroutineScope(Dispatchers.IO).launch {
                theftCommand.execute(emptyList(), dummyTransport)
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        val settings = SettingsRepository.getInstance(context)

        // Cancel any ongoing vibrations
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            vibrator.cancel()
        }
    }

    private fun speakWarning(context: Context, message: String) {
        pendingMessage = message
        if (tts == null) {
            tts = TextToSpeech(context, this)
        } else if (isTtsInitialized) {
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "WarningTTS")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsInitialized = true
            pendingMessage?.let {
                tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "WarningTTS")
                pendingMessage = null
            }
        }
    }
}
