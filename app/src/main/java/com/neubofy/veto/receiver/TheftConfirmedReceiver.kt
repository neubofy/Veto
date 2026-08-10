package com.neubofy.veto.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.services.TheftModeService
import com.neubofy.veto.ui.TheftSuspectedActivity

class TheftConfirmedReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "veto_theft_channel"
        private const val NOTIFICATION_ID = 701
    }

    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsRepository.getInstance(context)
        settings.load()

        if (settings.get(Settings.SET_THEFT_MODE_ACTIVE) as Boolean) {
            settings.set(Settings.SET_THEFT_MODE_CONFIRMED, true)
            
            // 1. Start the Foreground Service for sensor monitoring
            val serviceIntent = Intent(context, TheftModeService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)

            // 2. Bring the app to the foreground by launching the activity via a high priority notification intent
            val activityIntent = Intent(context, TheftSuspectedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("isConfirmed", true)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context, 0, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Fire a notification that immediately tries to launch full screen intent, 
            // or if the screen is unlocked, at least shows the notification the user can tap
            createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_security)
                .setContentTitle("Theft Protection Active")
                .setContentText("Device is locked and tracking is active.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(false)
                .setOngoing(true)

            notificationManager.notify(NOTIFICATION_ID, builder.build())
            
            // Also directly start activity just in case we are in a state where we can
            try {
                context.startActivity(activityIntent)
            } catch (e: Exception) {
                // Ignore, full screen intent notification will handle it
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Theft Protection"
            val descriptionText = "Active theft protection status and alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(null, null) // Silent notification, disturb handles sound
                enableVibration(false)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
