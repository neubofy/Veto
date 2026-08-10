package com.neubofy.veto.commands

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.permissions.LocationPermission
import com.neubofy.veto.receiver.TheftConfirmedReceiver
import com.neubofy.veto.services.TheftModeService
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.ui.TheftSuspectedActivity
import com.neubofy.veto.utils.SecureSettings
import com.neubofy.veto.utils.log
import kotlinx.coroutines.launch

class TheftCommand(context: Context) : Command(context) {

    override val keyword = "theft"
    override val usage = "theft [end]"
    override val icon = R.drawable.ic_security
    override val shortDescription = R.string.command_theft_description
    override val requiredPermissions = listOf(LocationPermission())

    override internal suspend fun <T> executeInternal(args: List<String>, transport: Transport<T>) {
        val firstArg = args.firstOrNull()?.lowercase()
        if (firstArg == "end" || firstArg == "stop") {
            settings.set(Settings.SET_THEFT_MODE_ACTIVE, false)
            settings.set(Settings.SET_THEFT_MODE_CONFIRMED, false)
            
            // 1. Cancel Alarm
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, TheftConfirmedReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            
            // 2. Stop Service
            val serviceIntent = Intent(context, TheftModeService::class.java)
            context.stopService(serviceIntent)
            
            // 3. Finish Overlays
            val finishIntent = Intent("com.neubofy.veto.ACTION_FINISH_LOCK_SCREEN")
            context.sendBroadcast(finishIntent)
            
            transport.send(context, "Theft mode deactivated and system stopped.", keyword)
            return
        }

        // Anti-duplication guard & Re-trigger logic
        if (settings.get(Settings.SET_THEFT_MODE_ACTIVE) as Boolean) {
            context.log().w("TheftCommand", "Theft mode is already active. Re-triggering as a bad event.")
            
            // Confirm theft and trigger bad event if re-triggered
            settings.set(Settings.SET_THEFT_MODE_CONFIRMED, true)
            val badEventIntent = Intent(context, TheftModeService::class.java).apply {
                action = "ACTION_BAD_EVENT_RE_TRIGGER"
            }
            androidx.core.content.ContextCompat.startForegroundService(context, badEventIntent)
            
            transport.send(context, "Theft mode is already active. Treated as a bad event (Theft Confirmed).", keyword)
            return
        }
        
        settings.set(Settings.SET_THEFT_MODE_ACTIVE, true)
        settings.set(Settings.SET_THEFT_MODE_CONFIRMED, false)

        // Enforce device state once
        enforceDeviceState()

        // Lock Device
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            if (dpm.isAdminActive(android.content.ComponentName(context, com.neubofy.veto.receiver.DeviceAdminReceiver::class.java))) {
                dpm.lockNow()
            }
        } catch (e: Exception) {
            context.log().e("TheftCommand", "Failed to lock device: ${e.message}")
        }

        // Show Suspected Overlay
        try {
            val activityIntent = Intent(context, TheftSuspectedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(activityIntent)
        } catch (e: Exception) {
            context.log().e("TheftCommand", "Failed to launch overlay: ${e.message}")
        }

        // Schedule 3-minute alarm
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, TheftConfirmedReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val durationMins = settings.get(Settings.SET_THEFT_SUSPECTED_DURATION) as Int
            val triggerTime = System.currentTimeMillis() + (durationMins * 60 * 1000L)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            context.log().e("TheftCommand", "Failed to schedule alarm: ${e.message}")
        }
        
        // Execute Locate Command in sequence
        try {
            val locateCmd = LocateCommand(context)
            val dummyTransport = com.neubofy.veto.transports.InAppTransport(context)
            locateCmd.executeInternal(emptyList(), dummyTransport)
        } catch (e: Exception) {
            context.log().e("TheftCommand", "Failed to trigger locate command: ${e.message}")
        }
        
        val durationMins = settings.get(Settings.SET_THEFT_SUSPECTED_DURATION) as Int
        transport.send(context, "Theft mode activated. Device locked, ${durationMins}-minute suspected phase started. Location requested.", keyword)
    }
    
    // Made public so it can be called from SimStateReceiver without needing Transport
    fun executeInternal(context: Context) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val dummyTransport = com.neubofy.veto.transports.InAppTransport(context)
            executeInternal(emptyList(), dummyTransport)
        }
    }

    private fun enforceDeviceState() {
        // GPS
        try {
            SecureSettings.turnGPS(context, true)
        } catch (e: Exception) {
            context.log().w("TheftCommand", "Failed to enable GPS: ${e.message}")
        }

        // DND Off
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (e: Exception) {
            context.log().w("TheftCommand", "Failed to disable DND: ${e.message}")
        }

        // Volume Max (Alarm Stream)
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )
        } catch (e: Exception) {
            context.log().w("TheftCommand", "Failed to max volume: ${e.message}")
        }

        // Bluetooth On
        try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            if (btAdapter != null && !btAdapter.isEnabled) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    @Suppress("DEPRECATION")
                    btAdapter.enable()
                } else {
                    SecureSettings.setBluetooth(context, true)
                }
            }
        } catch (e: Exception) {
            context.log().w("TheftCommand", "Failed to enable Bluetooth: ${e.message}")
        }
    }
}
