package com.neubofy.veto.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.utils.DisturbThief
import com.neubofy.veto.utils.TheftEventListener
import com.neubofy.veto.utils.TheftSensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TheftModeService : Service(), TheftEventListener {

    private lateinit var sensorManager: TheftSensorManager
    private lateinit var disturbThief: DisturbThief
    private lateinit var settings: SettingsRepository

    private var lastLocateTime = 0L

    companion object {
        private const val CHANNEL_ID = "veto_theft_service"
        private const val NOTIFICATION_ID = 702
        private const val WORK_LOCATE_NAME = "veto_theft_locate_worker"
        
        const val ACTION_GOOD_EVENT = "ACTION_GOOD_EVENT"
    }

    override fun onCreate() {
        super.onCreate()
        
        settings = SettingsRepository.getInstance(this)
        disturbThief = DisturbThief(this)
        sensorManager = TheftSensorManager(this, this)
        
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Theft Protection Active")
            .setContentText("Monitoring device state...")
            .setSmallIcon(R.drawable.ic_security)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
            
        startForeground(NOTIFICATION_ID, notification)
        
        sensorManager.register()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_GOOD_EVENT) {
            val type = intent.getStringExtra("type") ?: "unknown"
            onGoodEvent(type)
        } else if (intent?.action == "ACTION_BAD_EVENT_WRONG_PASS") {
            onBadEvent("wrong_password")
        }
        
        // If theft mode was disabled while we were starting up
        if (!(settings.get(Settings.SET_THEFT_MODE_ACTIVE) as Boolean)) {
            stopSelf()
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregister()
        disturbThief.stop()
        
        // Cancel the background location worker when theft mode stops
        WorkManager.getInstance(this).cancelUniqueWork(WORK_LOCATE_NAME)
    }

    override fun onBadEvent(type: String) {
        Log.d("TheftModeService", "Bad event detected: $type")
        
        if (!(settings.get(Settings.SET_THEFT_MODE_ACTIVE) as Boolean)) {
            stopSelf()
            return
        }
        
        val ringtoneUri = settings.get(Settings.SET_RINGER_TONE) as String
        disturbThief.start(ringtoneUri)

        // Intelligent Locate (Background Worker)
        val locateWork = PeriodicWorkRequestBuilder<com.neubofy.veto.workers.TheftLocateWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WORK_LOCATE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            locateWork
        )
    }

    override fun onGoodEvent(type: String) {
        Log.d("TheftModeService", "Good event detected: $type")
        disturbThief.stop()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Theft Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs the theft protection sensors"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
