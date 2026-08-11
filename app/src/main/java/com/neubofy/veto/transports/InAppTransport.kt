package com.neubofy.veto.transports

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.EditText
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neubofy.veto.R
import com.neubofy.veto.commands.ParserResult
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.permissions.PostNotificationsPermission
import com.neubofy.veto.receiver.CopyInAppTextReceiver
import com.neubofy.veto.receiver.EXTRA_TEXT_TO_COPY

import com.neubofy.veto.utils.Notifications
import com.neubofy.veto.workers.CommandExecutionWorker


class InAppTransport(
    private val context: Context,
) : Transport<Unit>(Unit) {

    @get:DrawableRes
    override val icon = R.drawable.ic_in_app

    @get:StringRes
    override val title = R.string.transport_inapp_title

    override val description = context.getString(R.string.transport_inapp_description)

    override val requiredPermissions = listOf(PostNotificationsPermission())

    override val actions = emptyList<TransportAction>()

    override fun getDestinationString(): String = context.getString(R.string.transport_inapp_title)

    override fun isAllowed(parsed: ParserResult.Success): Boolean {
        return true
    }

    override fun send(context: Context, msg: String, commandName: String?) {
        super.send(context, msg, commandName)

        val title = context.getString(R.string.transport_inapp_title)
        val encRepo = com.neubofy.veto.data.EncryptedSettingsRepository.getInstance(context)

        if (encRepo.isTransportEnabled("cloud") && NextJsServerTransport.isConnected(context)) {
            // Forward to server
            val serverTransport = NextJsServerTransport(context)
            serverTransport.send(context, msg, commandName)
            
            // Send short local notification
            Notifications.notify(
                context, 
                title, 
                "Command executed. View response on Dashboard.", 
                Notifications.CHANNEL_IN_APP
            ) { builder ->
                val url = "https://veto.neubofy.in/dashboard/console#logs"
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                val pi = PendingIntent.getActivity(
                    context, 
                    0, 
                    intent, 
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.setContentIntent(pi)
            }
        } else {
            Notifications.notify(context, title, msg, Notifications.CHANNEL_IN_APP)
        }
    }
}

@SuppressLint("SetTextI18n")
fun onTestCommandClicked(activity: AppCompatActivity) {
    val context = activity
    val dialogLayout = LayoutInflater.from(context).inflate(R.layout.dialog_in_app_command, null)
    val editTextCommand = dialogLayout.findViewById<EditText>(R.id.editTextCommand)

    val settings = SettingsRepository.getInstance(context)
    val vetoTriggerWord = settings.get(Settings.SET_Veto_COMMAND) as String
    editTextCommand.setText("$vetoTriggerWord ")

    MaterialAlertDialogBuilder(context)
        .setTitle(context.getString(R.string.transport_inapp_send_command_title))
        .setView(dialogLayout)
        .setPositiveButton(
            context.getString(R.string.transport_inapp_send_command_button_send)
        ) { _, _ ->
            val command = editTextCommand.text.toString()

            val inputData = workDataOf(
                CommandExecutionWorker.KEY_COMMAND to command,
                CommandExecutionWorker.KEY_TRANSPORT_TYPE to CommandExecutionWorker.TRANS_INAPP,
                CommandExecutionWorker.KEY_DESTINATION to context.getString(R.string.transport_inapp_title),
            )
            val workRequest = OneTimeWorkRequestBuilder<CommandExecutionWorker>()
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
        .setNegativeButton(context.getString(R.string.cancel), null)
        .show()
}
