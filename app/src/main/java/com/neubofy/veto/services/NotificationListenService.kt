package com.neubofy.veto.services

import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.neubofy.veto.VetoApplication
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.workers.CommandExecutionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class NotificationListenService : NotificationListenerService() {

    companion object {
        // Give other code in the Veto app access to this service via a static field,
        // but only while the service is connected to the NotificationManager.
        var instance: NotificationListenService? = null
    }

    private lateinit var settings: SettingsRepository

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        settings = SettingsRepository.getInstance(this)

        // SMS is handled separately
        val packageName = sbn.packageName
        if (packageName == Telephony.Sms.getDefaultSmsPackage(this)) {
            return
        }

        val messageChars = sbn.notification.extras.getCharSequence("android.text") ?: return
        val message = messageChars
            // Texts in notifications from SchildiChat start with this control character,
            // even if the message is otherwise ASCII-only (Android 16 GrapheneOS, Android 15 LineageOS).
            // HACK: Remove it, to get the notification transport working on a basic level.
            // This will break things that actually do need it (e.g., `veto mypin lock my-message-with-rtl-text`.
            // TODO: Come back to this and think about how to properly support Unicode.
            .trimStart('\u2068')
            .toString()

        // Early sanity check + abort
        val vetoTriggerWord = (settings.get(Settings.SET_Veto_COMMAND) as String)
        if (!message.startsWith(vetoTriggerWord, ignoreCase = true)) {
            return
        }

        (applicationContext as VetoApplication).latestStatusBarNotification = sbn

        val inputData = workDataOf(
            CommandExecutionWorker.KEY_COMMAND to message,
            CommandExecutionWorker.KEY_TRANSPORT_TYPE to CommandExecutionWorker.TRANS_NOTIFICATION_REPLY,
            CommandExecutionWorker.KEY_DESTINATION to sbn.packageName,
            CommandExecutionWorker.KEY_NOTIF_KEY to sbn.key,
        )
        val workRequest = OneTimeWorkRequestBuilder<CommandExecutionWorker>()
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}
