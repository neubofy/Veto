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

    private val handler = Handler(Looper.getMainLooper())

    private var isRegistered = false

    // Debounce tracking
    private var lastBadEventTime = 0L
    private val DEBOUNCE_MS = 3000L


    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    listener.onGoodEvent("charger_connect")
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    triggerBadEvent("charger_disconnect")
                }
                "android.hardware.usb.action.USB_DEVICE_ATTACHED" -> {
                    triggerBadEvent("usb_attached")
                }
                Intent.ACTION_SCREEN_ON -> {
                    triggerBadEvent("screen_on")
                }
                Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                    val isAirplaneModeOn = intent.getBooleanExtra("state", false)
                    if (isAirplaneModeOn) {
                        triggerBadEvent("airplane_mode_enabled")
                    }
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


    fun register() {
        if (isRegistered) return
        isRegistered = true

        sigMotionSensor?.let {
            sensorManager.requestTriggerSensor(sigMotionListener, it)
        }
        

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED")
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
        }
        context.registerReceiver(powerReceiver, filter)
    }

    fun unregister() {
        if (!isRegistered) return
        isRegistered = false

        sensorManager.cancelTriggerSensor(sigMotionListener, sigMotionSensor)
        
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
