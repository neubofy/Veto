package com.neubofy.veto.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.AutoTheftWarningOverlay
import com.neubofy.veto.utils.AutoTheftDefenseManager
import com.neubofy.veto.utils.log
import kotlinx.coroutines.*

class AutoTheftDefenseService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "AutoTheftDefenseService"
        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "channel_auto_theft_defense"

        private var isServiceRunning = false
        fun isRunning() = isServiceRunning

        fun startDefenseService(context: Context) {
            val intent = Intent(context, AutoTheftDefenseService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopDefenseService(context: Context) {
            val intent = Intent(context, AutoTheftDefenseService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var accelerometer: Sensor? = null

    private var initialLightValue = -1f
    private var isDisturbanceSirenRunning = false
    private var disturbanceJob: Job? = null
    private var graceTimerJob: Job? = null

    // Motion State Hysteresis (Pickup / Drop gesture tracking)
    private var isDeviceInMotion = false
    private var motionStillStartTime = System.currentTimeMillis()

    private var powerReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification("Auto-Theft Protection Active"))

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        registerSensors()
        registerPowerReceiver()

        AutoTheftDefenseManager.logTerminal(this, TAG, "Auto-Theft Defense Service Started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Engage Trackable Condition Toggles (DND OFF, RINGER 100%, GPS ON)
        serviceScope.launch(Dispatchers.IO) {
            AutoTheftDefenseManager.applyTrackableConditionToggles(this@AutoTheftDefenseService)
        }

        // 2. Start 3-Minute Grace Warning Window Timeline
        startGraceWarningTimeline()

        return START_STICKY
    }

    private fun startGraceWarningTimeline() {
        graceTimerJob?.cancel()
        graceTimerJob = serviceScope.launch {
            val settings = SettingsRepository.getInstance(this@AutoTheftDefenseService)
            val customTts = settings.get(Settings.SET_AUTO_THEFT_CUSTOM_TTS) as? String
                ?: "Theft suspected. Please unlock device to verify ownership."

            AutoTheftDefenseManager.logTerminal(this@AutoTheftDefenseService, TAG, "Phase 1: 3-Minute Grace Warning Window Active")

            // 0:00 to 0:30 — Speak Custom TTS Announcement 3 times
            AutoTheftDefenseManager.speakWarning(this@AutoTheftDefenseService, customTts)

            // Wait 3 minutes (180 seconds) for Grace Window to expire
            delay(180000L)

            // At 3:00 Mark — Automatically transition to Confirmed Theft Phase!
            AutoTheftDefenseManager.logTerminal(this@AutoTheftDefenseService, TAG, "Phase 2: THEFT CONFIRMED! Active Defense Sensors Engaged.")
            triggerBadJobEvent("3-minute grace window expired without unlock")
        }
    }

    /**
     * Good Job Events ONLY affect active disturbance sirens — they do NOT cancel running background command chains.
     */
    fun handleGoodJobEvent(reason: String) {
        AutoTheftDefenseManager.logTerminal(this, TAG, "Good Event: $reason (+Good Job)")
        if (isDisturbanceSirenRunning) {
            isDisturbanceSirenRunning = false
            disturbanceJob?.cancel()
            RingerService.stopRinging(this)
            AutoTheftDefenseManager.logTerminal(this, TAG, "Disturbance Siren Paused")
        }
    }

    fun triggerBadJobEvent(reason: String) {
        AutoTheftDefenseManager.logTerminal(this, TAG, "Bad Event: $reason (-Bad Job)")

        // 1. Trigger 30s Siren + Flash Blinking if not already running
        if (!isDisturbanceSirenRunning) {
            isDisturbanceSirenRunning = true
            RingerService.startRinging(this, 30)

            disturbanceJob?.cancel()
            disturbanceJob = serviceScope.launch {
                val flashCmd = com.neubofy.veto.commands.FlashCommand(this@AutoTheftDefenseService)
                val inApp = com.neubofy.veto.transports.InAppTransport(this@AutoTheftDefenseService)
                try {
                    flashCmd.execute(emptyList(), inApp)
                } catch (_: Exception) {}
                delay(30000)
                isDisturbanceSirenRunning = false
            }
        }

        // 2. Execute 5-minute throttled Bad Event Command Chain (locate, stats, photo front, photo back)
        serviceScope.launch(Dispatchers.IO) {
            AutoTheftDefenseManager.executeBadEventCommandChain(this@AutoTheftDefenseService)
        }
    }

    private fun registerSensors() {
        lightSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        proximitySensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    private fun registerPowerReceiver() {
        powerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> handleGoodJobEvent("Charger Connected")
                    Intent.ACTION_POWER_DISCONNECTED -> triggerBadJobEvent("Charger Disconnected")
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerReceiver, filter)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                if (initialLightValue < 0) {
                    initialLightValue = lux
                } else if (kotlin.math.abs(lux - initialLightValue) > 60f) {
                    initialLightValue = lux
                    triggerBadJobEvent("Ambient light shift ($lux lux)")
                }
            }
            Sensor.TYPE_PROXIMITY -> {
                val dist = event.values[0]
                if (dist < (proximitySensor?.maximumRange ?: 5f)) {
                    handleGoodJobEvent("Device proximity covered")
                } else {
                    triggerBadJobEvent("Proximity sensor uncovered (Device picked up)")
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val g = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val delta = kotlin.math.abs(g - SensorManager.GRAVITY_EARTH)

                // Hysteresis Motion State Machine (Pickup vs Flat/Drop)
                if (delta > 3.5f) { // High motion threshold = Pickup
                    motionStillStartTime = System.currentTimeMillis()
                    if (!isDeviceInMotion) {
                        isDeviceInMotion = true
                        triggerBadJobEvent("Device picked up / moved")
                    }
                } else if (delta < 1.2f) { // Low motion threshold = Flat / Still
                    val now = System.currentTimeMillis()
                    if (isDeviceInMotion && (now - motionStillStartTime > 3000L)) { // Still for 3s
                        isDeviceInMotion = false
                        handleGoodJobEvent("Device placed flat & stable")
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto-Theft Defense Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Keeps auto-theft active defense running in foreground"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(titleText: String): Notification {
        val intent = Intent(this, AutoTheftWarningOverlay::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText("Veto active defense monitoring device security.")
            .setSmallIcon(R.drawable.ic_security)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        graceTimerJob?.cancel()
        disturbanceJob?.cancel()
        sensorManager?.unregisterListener(this)
        powerReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        AutoTheftDefenseManager.stopTts()
        AutoTheftDefenseManager.logTerminal(this, TAG, "Auto-Theft Defense Service Stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
