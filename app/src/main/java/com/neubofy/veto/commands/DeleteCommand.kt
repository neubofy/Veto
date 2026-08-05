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

        val isValid = if (expectedPassword.startsWith("\$argon2id\$")) {
            com.neubofy.veto.utils.CypherUtils.checkPasswordForDelete(expectedPassword, pwd)
        } else {
            // Legacy unhashed or plaintext match fallback: verify and automatically upgrade to Argon2id
            val match = expectedPassword == pwd
            if (match) {
                encSettings.setDeletePassword(pwd)
            }
            match
        }

        if (!isValid) {
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

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    devicePolicyManager.wipeDevice(0)
                } else {
                    devicePolicyManager.wipeData(0)
                }
            } catch (e: Exception) {
                context.log().e(TAG, e.stackTraceToString())

                val msg = context.getString(R.string.cmd_delete_response_failed)
                context.log().i(TAG, msg)
                transport.send(context, msg, keyword)
            }
        }
    }
}
