package com.neubofy.veto.commands

import android.content.Context
import android.content.Intent
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.permissions.LocationPermission
import com.neubofy.veto.transports.Transport

class TheftCommand(context: Context) : Command(context) {

    override val keyword = "theft"
    override val usage = "theft"
    override val icon = R.drawable.ic_security
    override val shortDescription = R.string.command_theft_description
    override val requiredPermissions = listOf(LocationPermission())

    override internal suspend fun <T> executeInternal(args: List<String>, transport: Transport<T>) {
        val context = context
        val settings = SettingsRepository.getInstance(context)
        
        settings.set(Settings.SET_THEFT_MODE_ACTIVE, true)

        // Trigger Location Update first
        val locateCommand = LocateCommand(context)
        locateCommand.execute(listOf("gps"), transport)

        // Enable Bluetooth (requested)
        val bluetoothCommand = BluetoothCommand(context)
        bluetoothCommand.execute(listOf("on"), transport)

        // Disable DND (requested)
        val dndCommand = NoDisturbCommand(context)
        dndCommand.execute(listOf("off"), transport)

        // Trigger Ring Command for looping Ring and locking
        val ringCommand = RingCommand(context)
        ringCommand.execute(listOf("long"), transport)
        
        transport.send(context, context.getString(R.string.command_theft_description), keyword)
    }
}
