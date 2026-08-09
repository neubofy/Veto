package com.neubofy.veto.commands

import android.content.Context
import com.neubofy.veto.R
import com.neubofy.veto.data.EncryptedSettingsRepository
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.AutoTheftManager
import com.neubofy.veto.utils.Notifications
import com.neubofy.veto.utils.log

const val TAG = "CommandHandler"

internal fun availableCommands(context: Context): List<Command> {
    val commands = mutableListOf(
        LocateCommand(context),
        RingCommand(context),
        GpsCommand(context),
        NoDisturbCommand(context),
        RingerModeCommand(context),
        LockCommand(context),
        DeleteCommand(context),
        StatsCommand(context),
        FlashCommand(context),
        CameraCommand(context),
        StopCommand(context),
        TheftCommand(context),

        AudioCommand(context),
        VideoCommand(context),
    )
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

                // If Auto-Theft Warning is currently running, receiving a valid remote command acts as an explicit override/stop signal!
                val isAutoTheftActive = settings.get(Settings.SET_AUTO_THEFT_WARNING_ACTIVE) as? Boolean ?: false
                if (isAutoTheftActive) {
                    context.log().w(TAG, "Incoming remote command received during Auto-Theft Warning mode — cancelling warning state.")
                    AutoTheftManager.cancelSuspectedMode(context)
                }

                if (showUsageNotification) {
                    showUsageNotification(context, rawCommand)
                }
                onHandlingStarted()
                parsed.command.execute(parsed.args, transport)
            }

            else -> {
                context.log().w(TAG, "Command parsing did not result in execution: $rawCommand")
            }
        }
    }

    private fun showUsageNotification(context: Context, command: String) {
        val title = "Command executed"
        val msg = "Executed command: $command"

        Notifications.notify(
            context,
            title,
            msg,
            Notifications.CHANNEL_IN_APP,
        )
    }
}
