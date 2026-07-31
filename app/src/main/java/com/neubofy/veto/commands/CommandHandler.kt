package com.neubofy.veto.commands

import android.content.Context
import com.neubofy.veto.R
import com.neubofy.veto.data.EncryptedSettingsRepository
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.Notifications
import com.neubofy.veto.utils.log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job


// Order matters for the home screen
fun availableCommands(context: Context): List<Command> {
    val commands = mutableListOf(
        BluetoothCommand(context),
        CameraCommand(context),
        DeleteCommand(context),
        FlashCommand(context),
        GpsCommand(context),
        // HelpCommand(context),
        LocateCommand(context),
        LockCommand(context),
        NoDisturbCommand(context),
        RingCommand(context),
        RingerModeCommand(context),
        StatsCommand(context),
        TheftCommand(context),
        AutoLocCommand(context),
        AudioCommand(context),
        VideoCommand(context),
    )
    // FIXME: The HelpCommand does not know about itself
    commands.add(HelpCommand(commands, context))
    return commands
}

/**
 * CommandHandler is the entry point for taking a string,
 * mapping it to a Command, and executing the command.
 *
 * Access control is done internally, after parsing the command.
 */
class CommandHandler<T>
@JvmOverloads constructor(
    private val transport: Transport<T>,
    private val showUsageNotification: Boolean = true,
) {

    /**
     * Parses and executes a command of the form "triggerWord command options", e.g. "veto locate cell"
     */
    @JvmOverloads
    suspend fun execute(
        context: Context,
        rawCommand: String,
        onHandlingStarted: () -> Unit = {},
    ) {
        val settings = SettingsRepository.getInstance(context)
        val vetoTriggerWord = settings.get(Settings.SET_Veto_COMMAND) as String

        val encSettings = EncryptedSettingsRepository.getInstance(context)
        val expectedPin = encSettings.getVetoPin()

        val cmds = availableCommands(context)
        val parser =
            CommandParser(vetoTriggerWord, expectedPin, HelpCommand(cmds, context), cmds)
        val parsed = parser.parse(rawCommand)

        when (parsed) {
            is ParserResult.Success -> {
                context.log().d(TAG, "Executing command: ${parsed.command.keyword}")
                if (!transport.isAllowed(parsed)) {
                    context.log().e(TAG, "Aborting, the transport denied the access.")
                    return
                }
                if (showUsageNotification) {
                    showUsageNotification(context, rawCommand)
                }

                // Only call this if we are actually handling the command, and not aborting.
                onHandlingStarted()

                // Register onStopped handler
                currentCoroutineContext().job.invokeOnCompletion {
                    parsed.command.onExecuteStopped()
                    transport.closeChannel()
                }

                parsed.command.execute(parsed.args, transport)
                // Cleanup should be run by the handler above
            }

            is ParserResult.Empty -> {
                context.log().w(TAG, "Cannot handle: args is empty.")
            }

            is ParserResult.TriggerWordMismatch -> {
                context.log().w(
                    TAG,
                    "Not handling: '${parsed.actual}' does not match trigger word '${parsed.expected}'"
                )
            }

            is ParserResult.UnknownCommand -> {
                context.log().w(TAG, "No command found that matches '${parsed.commandKeyword}'")
            }
        }
    }

    private fun showUsageNotification(context: Context, rawCommand: String) {
        val source = transport.getDestinationString()
        Notifications.notify(
            context,
            context.getString(R.string.usage_notification_title),
            context.getString(R.string.usage_notification_text_source, rawCommand, source),
            Notifications.CHANNEL_USAGE
        )
    }

    companion object {
        val TAG = CommandHandler::class.simpleName
    }
}
