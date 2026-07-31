package com.neubofy.veto.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.permissions.DeviceAdminPermission
import com.neubofy.veto.permissions.OverlayPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.ui.LockScreenMessage


class LockCommand(context: Context) : Command(context) {

    override val keyword = "lock"
    override val usage = "lock [msg]"

    @get:DrawableRes
    override val icon = R.drawable.ic_phone_lock

    @get:StringRes
    override val shortDescription = R.string.cmd_lock_description_short

    override val longDescription = R.string.cmd_lock_description_long

    override val requiredPermissions = listOf(DeviceAdminPermission())

    override val optionalPermissions = listOf(OverlayPermission())

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val devicePolicyManager =
            context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        devicePolicyManager.lockNow()

        var customMessage = args.joinToString(" ")

        if (customMessage.isEmpty()) {
            customMessage = settings.get(Settings.SET_LOCKSCREEN_MESSAGE) as String
        }

        // Only show the full-screen activity if there is a message. This allows you to silently
        // lock your device (by not providing a message) without alerting the potential thief.
        if (customMessage.isNotEmpty() && OverlayPermission().isGranted(context)) {
            val lockScreenMessage = Intent(context, LockScreenMessage::class.java)
            lockScreenMessage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // TODO: bring back passing this data??
            //lockScreenMessage.putExtra(LockScreenMessage.SENDER, transport.getDestinationString())
            //lockScreenMessage.putExtra(LockScreenMessage.SENDER_TYPE, ch.getSender().SENDER_TYPE)
            lockScreenMessage.putExtra(LockScreenMessage.CUSTOM_TEXT, customMessage)
            context.startActivity(lockScreenMessage)
        }

        transport.send(context, context.getString(R.string.cmd_lock_response), keyword)
    }
}
