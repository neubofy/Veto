package com.neubofy.veto.commands

import android.content.Context
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.permissions.LocationPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.log

class TheftCommand(context: Context) : Command(context) {

    override val keyword = "theft"
    override val usage = "theft [end]"
    override val icon = R.drawable.ic_security
    override val shortDescription = R.string.command_theft_description
    override val requiredPermissions = listOf(LocationPermission())

    override internal suspend fun <T> executeInternal(args: List<String>, transport: Transport<T>) {
        val firstArg = args.firstOrNull()?.lowercase()
        if (firstArg == "end" || firstArg == "stop") {
            settings.set(Settings.SET_THEFT_MODE_ACTIVE, false)
            com.neubofy.veto.services.RingerService.stopRinging(context)
            
            // Send broadcast to finish LockScreenActivity if it's active
            val intent = android.content.Intent("com.neubofy.veto.ACTION_FINISH_LOCK_SCREEN")
            context.sendBroadcast(intent)
            
            transport.send(context, "Theft mode deactivated and alarm stopped.", keyword)
            return
        }
        
        settings.set(Settings.SET_THEFT_MODE_ACTIVE, true)

        // 1. Enable GPS explicitly
        try {
            val gpsCommand = GpsCommand(context)
            gpsCommand.execute(listOf("on"), transport)
        } catch (e: Exception) {
            context.log().w("TheftCommand", "GpsCommand in TheftMode failed: ${e.message}")
        }

        // 2. Trigger Location Update
        try {
            val locateCommand = LocateCommand(context)
            locateCommand.execute(emptyList(), transport)
        } catch (e: Exception) {
            context.log().w("TheftCommand", "LocateCommand in TheftMode failed: ${e.message}")
        }

        // 3. Enable Bluetooth
        try {
            val bluetoothCommand = BluetoothCommand(context)
            bluetoothCommand.execute(listOf("on"), transport)
        } catch (e: Exception) {
            context.log().w("TheftCommand", "BluetoothCommand in TheftMode failed: ${e.message}")
        }

        // 4. Disable DND
        try {
            val dndCommand = NoDisturbCommand(context)
            dndCommand.execute(listOf("off"), transport)
        } catch (e: Exception) {
            context.log().w("TheftCommand", "NoDisturbCommand in TheftMode failed: ${e.message}")
        }

        // 5. Trigger Ring Command (Alarm Siren + Lock + 100% Volume Loop)
        try {
            val ringCommand = RingCommand(context)
            ringCommand.execute(emptyList(), transport)
        } catch (e: Exception) {
            context.log().w("TheftCommand", "RingCommand in TheftMode failed: ${e.message}")
        }
        
        transport.send(context, context.getString(R.string.command_theft_description), keyword)
    }
}
