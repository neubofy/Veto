package com.neubofy.veto.commands

import android.content.Context
import com.neubofy.veto.R
import com.neubofy.veto.services.RingerService
import com.neubofy.veto.transports.InAppTransport
import com.neubofy.veto.transports.NextJsServerTransport
import com.neubofy.veto.transports.Transport

class StopCommand(context: Context) : Command(context) {

    override val keyword = "stop"
    override val usage = "stop"
    override val icon = R.drawable.ic_security // or any suitable icon
    override val shortDescription = R.string.cmd_ring_description_short // We should probably use a specific string, but keeping it simple for now, or just provide a string directly.
    override val longDescription = null
    override val requiredPermissions = emptyList<com.neubofy.veto.permissions.Permission>()

    override suspend fun <T> executeInternal(args: List<String>, transport: Transport<T>) {
        // Stop ringing
        RingerService.stopRinging(context)

        // Cancel warning mode if active
        com.neubofy.veto.utils.AutoTheftManager.cancelSuspectedMode(context)

        transport.send(context, "All alarms and theft warnings have been stopped.", keyword)

        if (transport !is NextJsServerTransport) {
            try {
                val serverTransport = NextJsServerTransport(context)
                serverTransport.send(context, "🛑 All alarms and theft warnings have been stopped.", "stop_command")
            } catch (e: Exception) {
                // Ignore if server transport fails
            }
        }
    }
}
