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
import com.neubofy.veto.R
import com.neubofy.veto.commands.LocateCommand
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
        private const val LOCATE_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes
        
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
    }

    override fun onBadEvent(type: String) {
        Log.d("TheftModeService", "Bad event detected: $type")
        
        if (!(settings.get(Settings.SET_THEFT_MODE_ACTIVE) as Boolean)) {
            stopSelf()
            return
        }
        
        val ringtoneUri = settings.get(Settings.SET_RINGER_TONE) as String
        disturbThief.start(ringtoneUri)

        // Intelligent Locate
        val now = System.currentTimeMillis()
        if (now - lastLocateTime > LOCATE_COOLDOWN_MS) {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting) {
                lastLocateTime = now
                Log.d("TheftModeService", "Triggering locate command")
                
                CoroutineScope(Dispatchers.IO).launch {
                    val locateCmd = LocateCommand(this@TheftModeService)
                    val dummyTransport = com.neubofy.veto.transports.InAppTransport(this@TheftModeService)
                    locateCmd.execute(emptyList(), dummyTransport)
                }
            }
        }
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
