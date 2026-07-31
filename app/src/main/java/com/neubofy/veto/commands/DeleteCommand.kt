package com.neubofy.veto.commands

import android.app.admin.DevicePolicyManager
import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.data.EncryptedSettingsRepository
import com.neubofy.veto.data.Settings
import com.neubofy.veto.permissions.DeviceAdminPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.security.MessageDigest


class DeleteCommand(context: Context) : Command(context) {
    companion object {
        private val TAG = DeleteCommand::class.simpleName
    }

    override val keyword = "delete"
    override val usage = "delete <password> [dryrun]"

    @get:DrawableRes
    override val icon = R.drawable.ic_delete_outline

    @get:StringRes
    override val shortDescription = R.string.cmd_delete_description_short

    override val longDescription = R.string.cmd_delete_description_long

    override val requiredPermissions = listOf(DeviceAdminPermission())

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        if (!(settings.get(Settings.SET_WIPE_ENABLED) as Boolean)) {
            val msg = context.getString(R.string.cmd_delete_response_disabled)
            context.log().i(TAG, msg)
            transport.send(context, msg, keyword)
            return
        }

        if (args.isEmpty()) {
            val triggerWord = settings.get(Settings.SET_Veto_COMMAND) as String
            val fullUsage = "$triggerWord $usage"
            val msg = context.getString(R.string.cmd_delete_response_pwd_missing, fullUsage)
            context.log().i(TAG, msg)
            transport.send(context, msg, keyword)
            return
        }
        val pwd = args[0]

        val encSettings = EncryptedSettingsRepository.getInstance(context)
        val expectedPassword = encSettings.getDeletePassword()
        
        if (expectedPassword.isNullOrBlank()) {
            val msg = context.getString(R.string.cmd_delete_response_pwd_wrong)
            context.log().i(TAG, msg)
            transport.send(context, msg, keyword)
            return
        }

        val expectedHash = MessageDigest.getInstance("SHA-256")
            .digest(expectedPassword.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val isPlaintextMatch = MessageDigest.isEqual(expectedPassword.toByteArray(), pwd.toByteArray())
        val isHashMatch = MessageDigest.isEqual(expectedHash.toByteArray(), pwd.toByteArray())

        if (!isPlaintextMatch && !isHashMatch) {
            val msg = context.getString(R.string.cmd_delete_response_pwd_wrong)
            context.log().i(TAG, msg)
            transport.send(context, msg, keyword)
            return
        }

        // Be defensive, match anything that might look right
        if (args.getOrNull(1)?.contains("dry") == true) {
            val msg = context.getString(R.string.cmd_delete_response_dry_run)
            context.log().i(TAG, msg)
            transport.send(context, msg, keyword)
            return
        }

        withContext(Dispatchers.IO) {
            context.log().i(TAG, "Deleting device...")
            transport.send(context, context.getString(R.string.cmd_delete_response_success), keyword)

            // Give the message some time to be sent before the device is wiped
            delay(3000)

            val devicePolicyManager =
                context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

            // TODO: Use wipeDevice(), otherwise it won't work with targetSDK >= 34
            // See https://gitlab.com/Neubofy/veto/-/issues/199#note_1975457249
            // and https://gitlab.com/Neubofy/veto/-/issues/220
            try {
                devicePolicyManager.wipeData(0)
            } catch (e: Exception) {
                context.log().e(TAG, e.stackTraceToString())

                val msg = context.getString(R.string.cmd_delete_response_failed)
                context.log().i(TAG, msg)
                transport.send(context, msg, keyword)
            }
        }
    }
}
