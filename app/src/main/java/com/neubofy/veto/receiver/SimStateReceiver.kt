package com.neubofy.veto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.speech.tts.TextToSpeech
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import java.util.Locale

class SimStateReceiver : BroadcastReceiver(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingMessage: String? = null

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.SIM_STATE_CHANGED") {
            val state = intent.getStringExtra("ss")
            if (state == "ABSENT") {
                // Sim card removed
                val settings = SettingsRepository.getInstance(context)

                // Vibrate
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator.hasVibrator()) {
                    val pattern = longArrayOf(0, 1000, 1000)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                }

                // Lock phone
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                if (dpm.isAdminActive(android.content.ComponentName(context, DeviceAdminReceiver::class.java))) {
                    dpm.lockNow()
                }

                speakWarning(context, "Theft suspected. Please unlock to verify you are the owner.")
            }
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
