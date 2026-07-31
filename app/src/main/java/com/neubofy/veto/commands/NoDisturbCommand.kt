package com.neubofy.veto.commands

import android.app.NotificationManager
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.permissions.DoNotDisturbAccessPermission
import com.neubofy.veto.transports.Transport


class NoDisturbCommand(context: Context) : Command(context) {

    override val keyword = "nodisturb"
    override val usage = "nodisturb [on | off]"

    @get:DrawableRes
    override val icon = R.drawable.ic_do_not_disturb

    @get:StringRes
    override val shortDescription = R.string.cmd_nodisturb_description_short

    override val longDescription = null

    override val requiredPermissions = listOf(DoNotDisturbAccessPermission())

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (args.contains("on")) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            transport.send(context, context.getString(R.string.cmd_nodisturb_response_on), keyword)
        } else if (args.contains("off")) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            transport.send(context, context.getString(R.string.cmd_nodisturb_response_off), keyword)
        }
    }
}
