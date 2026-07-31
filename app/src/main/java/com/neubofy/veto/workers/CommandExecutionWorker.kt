package com.neubofy.veto.workers

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neubofy.veto.VetoApplication
import com.neubofy.veto.R
import com.neubofy.veto.commands.CommandHandler
import com.neubofy.veto.services.ServerLocationUploadService.SOURCE_REGULAR_BACKGROUND_UPLOAD
// import com.neubofy.veto.transports.VetoServerTransport
import com.neubofy.veto.transports.InAppTransport
import com.neubofy.veto.transports.NotificationReplyTransport
import com.neubofy.veto.transports.SmsTransport
import com.neubofy.veto.utils.Notifications
import com.neubofy.veto.utils.log
import kotlin.random.Random

class CommandExecutionWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    companion object {
        // Required
        const val KEY_COMMAND = "KEY_COMMAND"
        const val KEY_TRANSPORT_TYPE = "KEY_TRANSPORT_TYPE"
        const val KEY_DESTINATION = "KEY_DESTINATION"

        // Command-specific
        const val KEY_SMS_SUBSCRIPTION_ID = "KEY_SMS_SUBSCRIPTION_ID"
        const val KEY_NOTIF_KEY = "KEY_NOTIF_KEY"

        // Transport types
        const val TRANS_SMS = "TRANS_SMS"
        const val TRANS_NOTIFICATION_REPLY = "TRANS_NOTIFICATION_REPLY"
        const val TRANS_Veto_SERVER = "TRANS_Veto_SERVER"
        const val TRANS_INAPP = "TRANS_INAPP"
        const val TRANS_NEXTJS_SERVER = "TRANS_NEXTJS_SERVER"

        private val TAG = CommandExecutionWorker::class.simpleName
    }

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    override suspend fun doWork(): Result {
        // required by all transports
        val command = inputData.getString(KEY_COMMAND) ?: return Result.failure()
        val transportType = inputData.getString(KEY_TRANSPORT_TYPE) ?: return Result.failure()
        val destination = inputData.getString(KEY_DESTINATION) ?: "Unknown destination"

        applicationContext.log()
            .i(
                TAG,
                "Starting worker $id for transport=$transportType, destination=$destination, command=$command"
            )

        val commandHandler = createCommandHandler(transportType, destination)
            ?: return Result.failure()

        val foregroundInfo = createForegroundInfo(command, destination)
        try {
            setForeground(foregroundInfo)
        } catch (e: IllegalStateException) {
            applicationContext.log().w(TAG, "Cannot start as foreground: ${e.stackTraceToString()}")
            // Continue executing, and try work in background
        }

        commandHandler.execute(applicationContext, command)

        applicationContext.log().i(TAG, "Finishing worker $id")
        return Result.success()
    }

    fun createCommandHandler(
        transportType: String,
        destination: String
    ): CommandHandler<*>? {
        // not required by all transports
        val subscriptionId = inputData.getInt(KEY_SMS_SUBSCRIPTION_ID, 0)
        val notificationKey = inputData.getString(KEY_NOTIF_KEY) ?: ""

        val commandHandler: CommandHandler<*> = when (transportType) {
            TRANS_SMS -> {
                val transport = SmsTransport(applicationContext, destination, subscriptionId)
                CommandHandler<String>(transport, true)
            }

            TRANS_NOTIFICATION_REPLY -> {
                val cached = (applicationContext as VetoApplication).latestStatusBarNotification
                if (cached?.packageName != destination || cached.key != notificationKey) {
                    applicationContext.log().e(
                        TAG,
                        "Cached StatusBarNotification not up-to-date! "
                                + "${cached?.packageName} != $destination || "
                                + "${cached?.key} != $notificationKey"
                    )
                    return null
                }
                val transport = NotificationReplyTransport(applicationContext, cached)
                CommandHandler<StatusBarNotification?>(transport, true)
            }

            TRANS_Veto_SERVER -> {
                // val transport = VetoServerTransport(applicationContext, destination)
                // CommandHandler<Unit>(transport, destination != SOURCE_REGULAR_BACKGROUND_UPLOAD)
                return null
            }

            TRANS_INAPP -> {
                val transport = InAppTransport(applicationContext)
                CommandHandler<Unit>(transport, false)
            }

            TRANS_NEXTJS_SERVER -> {
                val transport = com.neubofy.veto.transports.NextJsServerTransport(applicationContext)
                CommandHandler<Unit>(transport, false)
            }

            else -> {
                applicationContext.log().e(TAG, "Invalid transport: $transportType")
                return null
            }
        }

        return commandHandler
    }

    private fun createForegroundInfo(command: String, destination: String): ForegroundInfo {
        val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        val title = applicationContext.getString(R.string.executing_title)
        val text = applicationContext.getString(R.string.executing_text, command, destination)
        val cancel = applicationContext.getString(android.R.string.cancel)

        val notification = NotificationCompat
            .Builder(applicationContext, Notifications.CHANNEL_EXECUTION_SERVICE.toString())
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setSmallIcon(R.drawable.veto_logo)
            .addAction(R.drawable.ic_cancel, cancel, intent)
            .build()

        val notificationId = Random.nextInt(100_000)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // TODO: Can we already distinguish here which type of service we will need?
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        return ForegroundInfo(notificationId, notification, type)
    }
}
