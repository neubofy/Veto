package com.neubofy.veto.services

import android.app.NotificationManager
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.Ringtone
import android.os.IBinder
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.utils.RingerUtils
import com.neubofy.veto.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RingerService : Service() {

    companion object {
        private val TAG = RingerService::class.java.simpleName
        private const val EXTRA_DURATION_SECS = "EXTRA_DURATION_SECS"

        fun startRinging(context: Context, durationSecs: Int = 180) {
            val intent = Intent(context, RingerService::class.java).apply {
                putExtra(EXTRA_DURATION_SECS, durationSecs)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRinging(context: Context) {
            val intent = Intent(context, RingerService::class.java)
            context.stopService(intent)
        }
    }

    private var ringtone: Ringtone? = null
    private var oldRingerMode: Int? = null
    private var oldAlarmVolume: Int? = null
    private var oldInterruptionFilter: Int? = null
    private var oldNotificationPolicy: NotificationManager.Policy? = null

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var isReceiverRegistered = false

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                context?.log()?.i(TAG, "Device unlocked by legitimate user (ACTION_USER_PRESENT). Stopping ringer.")
                val settings = SettingsRepository.getInstance(applicationContext)
                settings.set(Settings.SET_THEFT_MODE_ACTIVE, false)
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        this.log().i(TAG, "RingerService created")

        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(unlockReceiver, filter)
        isReceiverRegistered = true

        startForegroundNotification()
    }

    private fun startForegroundNotification() {
        val title = "Veto Alarm Siren Active"
        val text = "Alarm siren is ringing. Unlock device to stop."
        val notification = androidx.core.app.NotificationCompat.Builder(
            this,
            com.neubofy.veto.utils.Notifications.CHANNEL_EXECUTION_SERVICE.toString()
        )
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(com.neubofy.veto.R.drawable.veto_logo)
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .build()

        val notifId = 88192
        startForeground(notifId, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val durationSecs = intent?.getIntExtra(EXTRA_DURATION_SECS, 180) ?: 180

        val settings = SettingsRepository.getInstance(this)
        val ringtoneUriStr = settings.get(Settings.SET_RINGER_TONE) as String
        ringtone = RingerUtils.getRingtone(this, ringtoneUriStr)

        raiseVolumeToMax()
        ringtone?.play()

        val intervalSecs = settings.get(Settings.SET_VOLUME_ENFORCE_INTERVAL) as Int
        val intervalMs = intervalSecs * 1000L

        // Volume Lock Loop
        serviceScope.launch {
            val startTime = System.currentTimeMillis()
            val maxDurationMillis = durationSecs * 1000L

            while (true) {
                delay(intervalMs)
                raiseVolumeToMax()

                // Check if duration expired AND theft mode is not active
                val theftModeActive = settings.get(Settings.SET_THEFT_MODE_ACTIVE) as? Boolean ?: false
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= maxDurationMillis && !theftModeActive) {
                    this@RingerService.log().i(TAG, "Ringer duration expired ($durationSecs s). Stopping service.")
                    stopSelf()
                    break
                }
            }
        }

        return START_STICKY
    }

    private fun raiseVolumeToMax() {
        try {
            val audioManager = getSystemService(AudioManager::class.java)
            val notificationManager = getSystemService(NotificationManager::class.java)

            if (oldRingerMode == null) oldRingerMode = audioManager.ringerMode
            if (oldAlarmVolume == null) oldAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            if (oldInterruptionFilter == null) oldInterruptionFilter = notificationManager.currentInterruptionFilter
            if (oldNotificationPolicy == null) oldNotificationPolicy = notificationManager.notificationPolicy

            notificationManager.setInterruptionFilter(INTERRUPTION_FILTER_ALL)
        } catch (e: Exception) {
            this.log().e(TAG, "Error in raiseVolumeToMax: ${e.message}")
        }
    }

    private fun resetVolume() {
        try {
            val audioManager = getSystemService(AudioManager::class.java)
            val notificationManager = getSystemService(NotificationManager::class.java)

            oldRingerMode?.let { audioManager.ringerMode = it }
            oldAlarmVolume?.let { audioManager.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) }
            oldInterruptionFilter?.let { notificationManager.setInterruptionFilter(it) }
            oldNotificationPolicy?.let { notificationManager.notificationPolicy = it }
        } catch (e: Exception) {
            this.log().e(TAG, "Error in resetVolume: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        this.log().i(TAG, "RingerService destroying")
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(unlockReceiver)
            } catch (_: Exception) {}
            isReceiverRegistered = false
        }
        ringtone?.stop()
        resetVolume()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
