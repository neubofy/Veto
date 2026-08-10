package com.neubofy.veto.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.Handler
import android.os.Looper
import kotlin.math.sqrt

interface TheftEventListener {
    fun onBadEvent(type: String)
    fun onGoodEvent(type: String)
}

class TheftSensorManager(private val context: Context, private val listener: TheftEventListener) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sigMotionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)
    private val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val handler = Handler(Looper.getMainLooper())

    private var isRegistered = false

    // Debounce tracking
    private var lastBadEventTime = 0L
    private val DEBOUNCE_MS = 3000L

    // Accelerometer tracking
    private var isRelaxing = false
    private val relaxRunnable = Runnable {
        if (isRegistered && isRelaxing) {
            listener.onGoodEvent("relax")
        }
    }

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    listener.onGoodEvent("charger_connect")
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    triggerBadEvent("charger_disconnect")
                }
            }
        }
    }

    private val sigMotionListener = object : TriggerEventListener() {
        override fun onTrigger(event: TriggerEvent?) {
            if (isRegistered) {
                triggerBadEvent("motion")
                // Significant motion is a one-shot sensor, re-register
                sigMotionSensor?.let { sensorManager.requestTriggerSensor(this, it) }
            }
        }
    }

    private val accelListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event == null || !isRegistered) return

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gForce = sqrt((x * x + y * y + z * z).toDouble())

            if (gForce > 12.0) { // Significant spike (gravity is 9.8)
                isRelaxing = false
                handler.removeCallbacks(relaxRunnable)
                triggerBadEvent("pickup")
            } else if (gForce in 9.0..10.5) {
                if (!isRelaxing) {
                    isRelaxing = true
                    handler.postDelayed(relaxRunnable, 10_000)
                }
            } else {
                isRelaxing = false
                handler.removeCallbacks(relaxRunnable)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun register() {
        if (isRegistered) return
        isRegistered = true

        sigMotionSensor?.let {
            sensorManager.requestTriggerSensor(sigMotionListener, it)
        }
        
        accelSensor?.let {
            sensorManager.registerListener(accelListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(powerReceiver, filter)
    }

    fun unregister() {
        if (!isRegistered) return
        isRegistered = false

        sensorManager.cancelTriggerSensor(sigMotionListener, sigMotionSensor)
        sensorManager.unregisterListener(accelListener)
        
        handler.removeCallbacks(relaxRunnable)
        
        try {
            context.unregisterReceiver(powerReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }

    private fun triggerBadEvent(type: String) {
        val now = System.currentTimeMillis()
        if (now - lastBadEventTime > DEBOUNCE_MS) {
            lastBadEventTime = now
            listener.onBadEvent(type)
        }
    }
}
