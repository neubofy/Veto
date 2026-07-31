package com.neubofy.veto.commands

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.locationproviders.isLocationOn
import com.neubofy.veto.permissions.WriteSecureSettingsPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.SecureSettings


class GpsCommand(context: Context) : Command(context) {

    override val keyword = "gps"
    override val usage = "gps [on | off]"

    @get:DrawableRes
    override val icon = R.drawable.ic_satellite

    @get:StringRes
    override val shortDescription = R.string.cmd_gps_description_short

    override val longDescription = null

    override val requiredPermissions = listOf(WriteSecureSettingsPermission())

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        if (args.isEmpty()) {
            val msg = if (isLocationOn(context)){
                context.getString(R.string.cmd_gps_response_is_on)
            } else {
                context.getString(R.string.cmd_gps_response_is_off)
            }
            transport.send(context, msg, keyword)
        } else if (args.contains("on")) {
            SecureSettings.turnGPS(context, true)
            transport.send(context, context.getString(R.string.cmd_gps_response_on), keyword)
        } else if (args.contains("off")) {
            SecureSettings.turnGPS(context, false)
            transport.send(context, context.getString(R.string.cmd_gps_response_off), keyword)
        }
    }
}
