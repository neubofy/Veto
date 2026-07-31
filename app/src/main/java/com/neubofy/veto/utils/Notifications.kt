package com.neubofy.veto.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.neubofy.veto.R
import com.neubofy.veto.ui.MainActivity


object Notifications {
    private val TAG: String = Notifications::class.java.simpleName

    const val CHANNEL_USAGE: Int = 42

    // public static final int CHANNEL_LIFE = 43;
    const val CHANNEL_PIN: Int = 44
    const val CHANNEL_SERVER: Int = 45
    const val CHANNEL_SECURITY: Int = 46
    const val CHANNEL_FAILED: Int = 47
    const val CHANNEL_IN_APP: Int = 48
    const val CHANNEL_EXECUTION_SERVICE = 49

    @JvmStatic
    @JvmOverloads
    fun notify(
        context: Context,
        title: String?,
        text: String?,
        channelID: Int,
        cls: Class<*> = MainActivity::class.java,
        customizeBuilder: ((NotificationCompat.Builder) -> Unit) = { _ -> },
    ) {
        val builder = NotificationCompat.Builder(context, channelID.toString())
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))

        customizeBuilder(builder)

        val intent = Intent(context, cls)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        builder.setAutoCancel(true)
        builder.setContentIntent(pendingIntent)

        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        if (!granted) {
            context.log().e(TAG, "Cannot send notification: missing permission POST_NOTIFICATIONS")
            context.log().e(TAG, "$title: $text")
            return
        }

        val notificationId = System.currentTimeMillis().toInt() // any unique ID

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, builder.build())
    }

    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel1 = NotificationChannel(
                CHANNEL_USAGE.toString(),
                context.getString(R.string.Notification_Usage),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel1.description = context.getString(R.string.Notification_Usage_Description)

            val channel3 = NotificationChannel(
                CHANNEL_PIN.toString(),
                context.getString(R.string.Pin_Usage),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel3.description = context.getString(R.string.Notification_Pin_Usage_Description)

            val channel4 = NotificationChannel(
                CHANNEL_SERVER.toString(),
                context.getString(R.string.Notification_Server),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel4.description = context.getString(R.string.Notification_Server_Description)

            val channel5 = NotificationChannel(
                CHANNEL_SECURITY.toString(),
                context.getString(R.string.Notification_Security),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel5.description = context.getString(R.string.Notification_Security_Description)

            val channel6 = NotificationChannel(
                CHANNEL_FAILED.toString(),
                context.getString(R.string.Notification_FAIL),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel6.description = context.getString(R.string.Notification_Fail_Description)

            val channel7 = NotificationChannel(
                CHANNEL_IN_APP.toString(),
                context.getString(R.string.Notification_InApp),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel7.description = context.getString(R.string.Notification_InApp_Description)

            val channel8 = NotificationChannel(
                CHANNEL_EXECUTION_SERVICE.toString(),
                context.getString(R.string.Notification_ExecutionService),
                // Must be visible, but doesn't need to make a noise and interrupt the user.
                NotificationManager.IMPORTANCE_LOW
            )
            channel8.description =
                context.getString(R.string.Notification_ExecutionService_Description)

            val notificationManager =
                context.getSystemService<NotificationManager>(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel1)
            notificationManager.createNotificationChannel(channel3)
            notificationManager.createNotificationChannel(channel4)
            notificationManager.createNotificationChannel(channel5)
            notificationManager.createNotificationChannel(channel6)
            notificationManager.createNotificationChannel(channel7)
            notificationManager.createNotificationChannel(channel8)
        }
    }
}
