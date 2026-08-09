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
        val settings = com.neubofy.veto.data.SettingsRepository.getInstance(context)
        val kw = settings.get(com.neubofy.veto.data.Settings.SET_Veto_COMMAND) as String

        val reply = StringBuilder()
        reply.appendLine("Veto Command Guide:")
        reply.appendLine("-------------------")
        
        for (cmd in availableCommands) {
            when (cmd.keyword) {
                "theft" -> {
                    reply.appendLine("🚨 ${context.getString(cmd.shortDescription)}")
                    reply.appendLine("  Start: $kw theft")
                    reply.appendLine("  Stop:  $kw theft end")
                }
                "lock" -> {
                    reply.appendLine("🔒 ${context.getString(cmd.shortDescription)}")
                    reply.appendLine("  Lock with message: $kw lock Please call 911")
                }
                "ring" -> {
                    reply.appendLine("🔊 ${context.getString(cmd.shortDescription)}")
                    reply.appendLine("  Ring normally: $kw ring")
                    reply.appendLine("  Ring for 60s:  $kw ring 60")
                    reply.appendLine("  Stop ringing:  $kw ring stop")
                }
                "location" -> {
                    reply.appendLine("📍 ${context.getString(cmd.shortDescription)}")
                    reply.appendLine("  Get location: $kw location")
                }
                "flash" -> {
                    reply.appendLine("🔦 ${context.getString(cmd.shortDescription)}")
                    reply.appendLine("  Turn on:     $kw flash")
                    reply.appendLine("  Turn on 30s: $kw flash 30")
                    reply.appendLine("  Turn off:    $kw flash stop")
                }
                "battery" -> {
                    reply.appendLine("🔋 ${context.getString(cmd.shortDescription)}")
                    reply.appendLine("  Get status: $kw battery")
                }
                "info" -> {
                    reply.appendLine("📱 ${context.getString(cmd.shortDescription)}")
                    reply.appendLine("  Get info: $kw info")
                }
                "help" -> {
                    reply.appendLine("ℹ️ ${context.getString(cmd.shortDescription)}")
                    reply.appendLine("  Show this: $kw help")
                }
                else -> {
                    reply.appendLine("✨ ${context.getString(cmd.shortDescription)}")
                    reply.appendLine("  Run: $kw ${cmd.keyword}")
                }
            }
            reply.appendLine()
        }
        
        transport.send(context, reply.toString().trim(), keyword)
    }
}
