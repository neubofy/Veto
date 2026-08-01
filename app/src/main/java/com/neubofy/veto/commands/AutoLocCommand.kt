package com.neubofy.veto.commands

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.permissions.Permission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.workers.AutoLocWorker
import java.util.concurrent.TimeUnit

class AutoLocCommand(context: Context) : Command(context) {

    override val keyword = "autoloc"
    override val usage = "autoloc [on | off]"

    @get:DrawableRes
    override val icon = R.drawable.ic_location

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

        when (action) {
            "on" -> {
                val intervalMinutes = settings.get(Settings.SET_VetoSERVER_UPDATE_TIME) as Int
                val periodicWork = PeriodicWorkRequest.Builder(
                    AutoLocWorker::class.java,
                    intervalMinutes.toLong(),
                    TimeUnit.MINUTES
                ).build()
                workManager.enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicWork
                )
                if (transport !is com.neubofy.veto.transports.NextJsServerTransport) {
                    transport.send(context, "Background auto-location started (Interval: $intervalMinutes mins)", keyword)
                }
            }

            "off" -> {
                workManager.cancelUniqueWork(WORK_NAME)
                if (transport !is com.neubofy.veto.transports.NextJsServerTransport) {
                    transport.send(context, "Background auto-location stopped.", keyword)
                }
            }

            "run" -> {
                // Manual trigger from the in-app button — same logic as WorkManager periodic run
                val workRequest = OneTimeWorkRequest.Builder(AutoLocWorker::class.java).build()
                workManager.enqueue(workRequest)
                // Silent upload — no reply needed
            }

            else -> transport.send(context, "Invalid action. Usage: $usage", keyword)
        }
    }
}
