package com.neubofy.veto.commands

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.permissions.Permission
import com.neubofy.veto.transports.Transport


class HelpCommand(
    private val availableCommands: List<Command>,
    context: Context,
) : Command(context) {

    override val keyword = "help"
    override val usage = "help"

    @get:DrawableRes
    override val icon = R.drawable.ic_help

    @get:StringRes
    override val shortDescription = R.string.cmd_help_description_short

    override val longDescription = null

    override val requiredPermissions = emptyList<Permission>()

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val reply = StringBuilder()
        reply.appendLine(context.getString(R.string.cmd_help_message_start))
        reply.appendLine()
        for (cmd in availableCommands) {
            reply.appendLine("${cmd.usage} - ${context.getString(cmd.shortDescription)}")
        }
        transport.send(context, reply.toString(), keyword)
    }
}
