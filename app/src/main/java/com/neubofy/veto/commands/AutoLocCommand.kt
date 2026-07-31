package com.neubofy.veto.commands

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.workers.CommandExecutionWorker
import java.util.concurrent.TimeUnit
import com.neubofy.veto.permissions.Permission

class AutoLocCommand(context: Context) : Command(context) {

    override val keyword = "autoloc"
    override val usage = "autoloc [on | off]"

    @get:DrawableRes
    override val icon = R.drawable.ic_location
    
    // Using an existing string resource for simplicity, or we can use R.string.command_locate_description
    @get:StringRes
    override val shortDescription = R.string.cmd_locate_description_short

    override val longDescription = R.string.cmd_locate_description_long

    override val requiredPermissions: List<Permission> = emptyList()

    companion object {
        const val WORK_NAME = "VetoAutoLocWork"
    }

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        if (args.isEmpty()) {
            transport.send(context, "Usage: autoloc [on|off]", keyword)
            return
        }

        val action = args[0]
        val workManager = WorkManager.getInstance(context)

        if (action == "on") {
            val intervalMinutes = settings.get(Settings.SET_VetoSERVER_UPDATE_TIME) as Int
            val locateCommand = settings.get(Settings.SET_Veto_COMMAND).toString() + " locate gps"

            val inputData = Data.Builder()
                .putString(CommandExecutionWorker.KEY_COMMAND, locateCommand)
                .putString(CommandExecutionWorker.KEY_TRANSPORT_TYPE, CommandExecutionWorker.TRANS_Veto_SERVER)
                .putString(CommandExecutionWorker.KEY_DESTINATION, "Background_Upload")
                .build()

            val periodicWork = PeriodicWorkRequest.Builder(
                CommandExecutionWorker::class.java,
                intervalMinutes.toLong(),
                TimeUnit.MINUTES
            ).setInputData(inputData).build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, // Update existing job if any
                periodicWork
            )
            transport.send(context, "Background auto-location started (Interval: $intervalMinutes mins)", keyword)
        } else if (action == "off") {
            workManager.cancelUniqueWork(WORK_NAME)
            transport.send(context, "Background auto-location stopped.", keyword)
        } else {
            transport.send(context, "Invalid action. Usage: $usage", keyword)
        }
    }
}
