package com.neubofy.veto.commands

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.permissions.DoNotDisturbAccessPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.log


const val RING_DURATION_DEFAULT_SECS = 30
const val RING_DURATION_MAX_SECS = 30 * 60 // 30 minutes cap

class RingCommand(context: Context) : Command(context) {

    override val keyword = "ring"
    override val usage = "ring [seconds]"

    @get:DrawableRes
    override val icon = R.drawable.ic_volume_up

    @get:StringRes
    override val shortDescription = R.string.cmd_ring_description_short

    override val longDescription = null

    override val requiredPermissions = listOf(DoNotDisturbAccessPermission())

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val firstArg = args.getOrElse(0) { "" }

        var duration = RING_DURATION_DEFAULT_SECS
        if (firstArg.isNotEmpty()) {
            firstArg.toIntOrNull()?.let {
                duration = it.coerceIn(5, RING_DURATION_MAX_SECS)
            }
        }

        // 1. Lock screen using DevicePolicyManager directly (avoids full LockScreenMessage overlay)
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            if (dpm.isAdminActive(android.content.ComponentName(context, com.neubofy.veto.receiver.DeviceAdminReceiver::class.java))) {
                dpm.lockNow()
            }
        } catch (e: Exception) {
            context.log().w("RingCommand", "Failed to lock device: ${e.message}")
        }

        // 2. Start persistent RingerService (single, sole source of alarm ringing & 100% volume loop)
        com.neubofy.veto.services.RingerService.startRinging(context, duration)

        transport.send(context, context.getString(R.string.cmd_ring_response), keyword)
    }
}
