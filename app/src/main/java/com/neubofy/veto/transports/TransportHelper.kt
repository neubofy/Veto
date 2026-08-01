package com.neubofy.veto.transports

import android.content.Context
import android.content.Intent
import com.neubofy.veto.VetoApplication
import com.neubofy.veto.services.NotificationListenService
import com.neubofy.veto.workers.CommandExecutionWorker

object TransportHelper {
    const val EXTRA_TRANSPORT_TYPE = "EXTRA_TRANSPORT_TYPE"
    const val EXTRA_DESTINATION = "EXTRA_DESTINATION"
    const val EXTRA_SMS_SUBSCRIPTION_ID = "EXTRA_SMS_SUBSCRIPTION_ID"
    const val EXTRA_NOTIF_KEY = "EXTRA_NOTIF_KEY"

    fun attachTransportToIntent(intent: Intent, transport: Transport<*>) {
        when (transport) {
            is SmsTransport -> {
                intent.putExtra(EXTRA_TRANSPORT_TYPE, CommandExecutionWorker.TRANS_SMS)
                intent.putExtra(EXTRA_DESTINATION, transport.getDestinationString())
            }
            is NotificationReplyTransport -> {
                intent.putExtra(EXTRA_TRANSPORT_TYPE, CommandExecutionWorker.TRANS_NOTIFICATION_REPLY)
                intent.putExtra(EXTRA_DESTINATION, transport.getDestinationString())
            }
            is InAppTransport -> {
                intent.putExtra(EXTRA_TRANSPORT_TYPE, CommandExecutionWorker.TRANS_INAPP)
                intent.putExtra(EXTRA_DESTINATION, transport.getDestinationString())
            }
            is NextJsServerTransport -> {
                intent.putExtra(EXTRA_TRANSPORT_TYPE, CommandExecutionWorker.TRANS_NEXTJS_SERVER)
                intent.putExtra(EXTRA_DESTINATION, transport.getDestinationString())
            }
        }
    }

    fun getTransportFromIntent(context: Context, intent: Intent): Transport<*> {
        val type = intent.getStringExtra(EXTRA_TRANSPORT_TYPE) ?: CommandExecutionWorker.TRANS_NEXTJS_SERVER
        val destination = intent.getStringExtra(EXTRA_DESTINATION) ?: ""
        val subId = intent.getIntExtra(EXTRA_SMS_SUBSCRIPTION_ID, -1)
        val notifKey = intent.getStringExtra(EXTRA_NOTIF_KEY) ?: ""

        return when (type) {
            CommandExecutionWorker.TRANS_SMS -> {
                SmsTransport(context, destination, subId)
            }
            CommandExecutionWorker.TRANS_NOTIFICATION_REPLY -> {
                val app = context.applicationContext as VetoApplication
                var cached = app.latestStatusBarNotification
                if (cached?.packageName != destination || cached?.key != notifKey) {
                    val activeNotifs = NotificationListenService.instance?.activeNotifications
                    cached = activeNotifs?.firstOrNull { it.key == notifKey }
                        ?: activeNotifs?.firstOrNull { it.packageName == destination }
                }
                NotificationReplyTransport(context, cached)
            }
            CommandExecutionWorker.TRANS_INAPP -> {
                InAppTransport(context)
            }
            else -> {
                NextJsServerTransport(context)
            }
        }
    }
}
