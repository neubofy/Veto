package com.neubofy.veto.utils

import android.app.NotificationManager
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.Ringtone
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DisturbThief(private val context: Context) {

    private var ringtone: Ringtone? = null
    private var isTorchOn = false
    private var torchJob: Job? = null
    private var timeoutJob: Job? = null
    private var volumeJob: Job? = null
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var vibrator: Vibrator? = null

    @Volatile
    var isActive = false
        private set

    init {
        try {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager?.getCameraCharacteristics(id)?.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            Log.e("DisturbThief", "Failed to init camera manager", e)
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun start(ringtoneUri: String) {
        if (isActive) return
        isActive = true

        // Play Sound
        try {
            raiseVolumeToMax()
            ringtone = RingerUtils.getRingtone(context, ringtoneUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("DisturbThief", "Failed to play sound", e)
        }

        // Re-launch UI if it was swiped away
        try {
            val settings = com.neubofy.veto.data.SettingsRepository.getInstance(context)
            val activityIntent = Intent(context, com.neubofy.veto.ui.TheftSuspectedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                val isConfirmed = settings.get(com.neubofy.veto.data.Settings.SET_THEFT_MODE_CONFIRMED) as Boolean
                putExtra("isConfirmed", isConfirmed)
            }
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            Log.e("DisturbThief", "Failed to launch overlay on disturb start: ${e.message}")
        }



        // Notify UI
        context.sendBroadcast(Intent("com.neubofy.veto.ACTION_DISTURB_START"))

        val settings = com.neubofy.veto.data.SettingsRepository.getInstance(context)
        val intervalSecs = settings.get(com.neubofy.veto.data.Settings.SET_VOLUME_ENFORCE_INTERVAL) as Int
        val intervalMs = intervalSecs * 1000L

        // Volume Lock Loop
        volumeJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(intervalMs)
                if (isActive) raiseVolumeToMax()
            }
        }

        // Vibrate
        try {
            val pattern = longArrayOf(0, 500, 500) // wait, vibrate, sleep
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("DisturbThief", "Failed to vibrate", e)
        }

        // Flash Torch (Blink)
        torchJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                while (isActive) {
                    toggleTorch(true)
                    delay(150)
                    toggleTorch(false)
                    delay(2000)
                }
            } finally {
                toggleTorch(false)
            }
        }

        // Auto Timeout (30 seconds)
        timeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(30_000)
            stop()
        }
    }

    private fun raiseVolumeToMax() {
        try {
            val audioManager = context.getSystemService(AudioManager::class.java)
            val notificationManager = context.getSystemService(NotificationManager::class.java)

            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
            notificationManager.setInterruptionFilter(INTERRUPTION_FILTER_ALL)
        } catch (e: Exception) {
            Log.e("DisturbThief", "Error in raiseVolumeToMax: ${e.message}")
        }
    }

    fun stop() {
        if (!isActive) return
        isActive = false

        try {
            ringtone?.stop()
            ringtone = null
        } catch (e: Exception) {
            Log.e("DisturbThief", "Failed to stop media player", e)
        }



        // Notify UI
        context.sendBroadcast(Intent("com.neubofy.veto.ACTION_DISTURB_STOP"))

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("DisturbThief", "Failed to cancel vibration", e)
        }

        volumeJob?.cancel()
        volumeJob = null

        torchJob?.cancel()
        torchJob = null

        timeoutJob?.cancel()
        timeoutJob = null

        toggleTorch(false)
    }

    private fun toggleTorch(on: Boolean) {
        if (cameraId == null) return
        try {
            cameraManager?.setTorchMode(cameraId!!, on)
            isTorchOn = on
        } catch (e: Exception) {
            Log.e("DisturbThief", "Failed to set torch to $on", e)
        }
    }
}
