package com.neubofy.veto.transports

import android.app.Notification
import android.app.PendingIntent.CanceledException
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.commands.ParserResult
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.permissions.NotificationAccessPermission
import com.neubofy.veto.services.NotificationListenService
import com.neubofy.veto.utils.log


class NotificationReplyTransport(
    private val context: Context,
    // should only be null for the availableTransports list
    private val destination: StatusBarNotification?
) : Transport<StatusBarNotification?>(destination) {

    companion object {
        private val TAG = NotificationReplyTransport::class.simpleName
    }

    private val settings = SettingsRepository.getInstance(context)

    @get:DrawableRes
    override val icon = R.drawable.ic_notifications

    @get:StringRes
    override val title = R.string.transport_notification_reply_title

    private val keyword = settings.get(Settings.SET_Veto_COMMAND) as String
    override val description =
        context.getString(R.string.transport_notification_reply_description, keyword)

    override val descriptionAuth =
        context.getString(R.string.transport_notification_reply_description_auth)

    override val requiredPermissions = listOf(NotificationAccessPermission())

    override val actions = listOf(
        TransportAction(R.string.transport_notification_select_apps_title) { activity ->
            showAppSelectionDialog(activity)
        }
    )

    override fun getDestinationString() = destination?.packageName ?: "Notification Response"

    override fun isAllowed(parsed: ParserResult.Success): Boolean {
        val pinAccessEnabled = settings.get(Settings.SET_ACCESS_VIA_PIN) as Boolean
        if (!pinAccessEnabled) {
            return false
        }
        return parsed.pin != null
    }

    override fun send(context: Context, msg: String, commandName: String?) {
        super.send(context, msg, commandName)

        if (destination == null) {
            context.log().w(TAG, "Cannot reply, destination is null!")
            return
        }

        try {
            sendQuickReply(context, destination.notification, msg)
        } catch (e: CanceledException) {
            context.log().e(TAG, "Failed to send message via notification reply")
            e.printStackTrace()
        }
    }

    private fun tryDismissNotification() {
        if (destination == null) {
            return
        }

        // As an additional fallback:
        // Try to dismiss the notification via a "mark as read" action, if it exists.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val actions = destination.notification.actions ?: emptyArray()
            for (action in actions) {
                if (action.semanticAction == Notification.Action.SEMANTIC_ACTION_MARK_AS_READ) {
                    try {
                        action.actionIntent?.send()
                    } catch (e: Exception) {
                        context.log()
                            .w(TAG, "Failed to mark_as_read: ${e.stackTraceToString()}")
                    }
                }
            }
        }

        // Only the NotificationListenService is allowed to dismiss another app's notification.
        NotificationListenService.instance?.cancelNotification(destination.key)
    }

    private fun sendQuickReply(context: Context, notification: Notification, message: String) {
        val actions = notification.actions ?: emptyArray()
        for (action in actions) {
            // context.log().d(TAG, "Checking action ${action.title}")
            var isReplyAction = false

            val resultsBundle = Bundle()
            val relevantRemoteInputs = mutableListOf<RemoteInput>()

            var isSemanticQuickReply = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isSemanticQuickReply =
                    action.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY
            }

            val remoteInputs = action.remoteInputs ?: emptyArray<RemoteInput>()
            for (input in remoteInputs) {
                if (isSemanticQuickReply || input.resultKey.contains("reply")) {
                    isReplyAction = true
                    resultsBundle.putCharSequence(input.resultKey, message)
                    relevantRemoteInputs.add(input)
                }
            }

            if (isReplyAction) {
                val intent = Intent()
                RemoteInput.addResultsToIntent(
                    relevantRemoteInputs.toTypedArray(),
                    intent,
                    resultsBundle
                )

                // Dismiss the notification BEFORE sending the response.
                // If we first send a response and then dismiss, the messenger app may post a NEW notification
                // that includes the original message, causing infinite command execution loops.
                // The pending intent to sent the response should still work, despite this ordering.
                tryDismissNotification()

                action.actionIntent.send(context, 0, intent)
                return
            }
        }

        context.log().w(TAG, "Could not sent reply, no suitable Action or RemoteInput found.")
    }
}

fun showAppSelectionDialog(activity: androidx.appcompat.app.AppCompatActivity) {
    val settings = SettingsRepository.getInstance(activity)
    val currentStr = settings.get(Settings.SET_ALLOWED_NOTIFICATION_PACKAGES) as? String ?: ""
    val selectedPackages = currentStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()

    val knownApps = listOf(
        "WhatsApp" to "com.whatsapp",
        "WhatsApp Business" to "com.whatsapp.w4b",
        "Telegram" to "org.telegram.messenger",
        "Instagram" to "com.instagram.android",
        "Signal" to "org.thoughtcrime.securesms",
        "Facebook Messenger" to "com.facebook.orca",
        "Google Messages" to "com.google.android.apps.messaging",
        "Slack" to "com.Slack"
    )

    val appNames = knownApps.map { it.first }.toTypedArray()
    val checkedItems = knownApps.map { selectedPackages.contains(it.second) }.toBooleanArray()

    com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
        .setTitle(activity.getString(R.string.transport_notification_select_apps_title))
        .setMultiChoiceItems(appNames, checkedItems) { _, which, isChecked ->
            val pkg = knownApps[which].second
            if (isChecked) selectedPackages.add(pkg) else selectedPackages.remove(pkg)
        }
        .setPositiveButton(android.R.string.ok) { _, _ ->
            settings.set(Settings.SET_ALLOWED_NOTIFICATION_PACKAGES, selectedPackages.joinToString(","))
            android.widget.Toast.makeText(activity, "Messaging app list saved.", android.widget.Toast.LENGTH_SHORT).show()
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}
