package com.neubofy.veto.transports

import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.commands.ParserResult
import com.neubofy.veto.data.AllowlistRepository
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.data.TEMP_USAGE_VALIDITY_MILLIS
import com.neubofy.veto.data.TemporaryAllowlistRepository
import com.neubofy.veto.permissions.SmsPermission
import com.neubofy.veto.services.TempContactExpiredService
import com.neubofy.veto.ui.settings.AllowlistActivity
import com.neubofy.veto.utils.Notifications
import com.neubofy.veto.utils.log


class SmsTransport(
    private val context: Context,
    private val phoneNumber: String,
    private val subscriptionId: Int
) : Transport<String>(phoneNumber) {

    companion object {
        private val TAG = SmsTransport::class.simpleName
    }

    private val settings = SettingsRepository.getInstance(context)
    private val allowlistRepo = AllowlistRepository.getInstance(context)
    private val tempAllowlistRepo = TemporaryAllowlistRepository.getInstance(context)

    @get:DrawableRes
    override val icon = R.drawable.ic_sms

    @get:StringRes
    override val title = R.string.transport_sms_title

    private val keyword = settings.get(Settings.SET_Veto_COMMAND) as String
    override val description = context.getString(R.string.transport_sms_description, keyword)

    override val descriptionAuth = context.getString(R.string.transport_sms_description_auth, keyword)

    override val descriptionNote = context.getString(R.string.transport_sms_description_note)

    override val requiredPermissions = listOf(SmsPermission())

    override val actions = listOf(TransportAction(R.string.Settings_WhiteList) { activity ->
        activity.startActivity(Intent(context, AllowlistActivity::class.java))
    })

    override fun getDestinationString() = phoneNumber

    override fun isAllowed(parsed: ParserResult.Success): Boolean {
        // Case 1: phone number in Allowed Contacts
        if (allowlistRepo.containsNumber(phoneNumber)) {
            context.log().i(TAG, "$phoneNumber used Veto via allowlist")
            return true
        }

        // Case 2: phone number in temporary allowlist (i.e., it send the correct PIN earlier)
        val pinAccessEnabled = settings.get(Settings.SET_ACCESS_VIA_PIN) as Boolean
        if (pinAccessEnabled) {
            if (tempAllowlistRepo.containsValidNumber(phoneNumber)) {
                context.log().i(TAG, "$phoneNumber used Veto via temporary allowlist")
                return true
            }

            // Case 3: the message contains the correct PIN
            if (parsed.pin != null) {
                context.log().i(TAG, "$phoneNumber used Veto via PIN")
                send(context, context.getString(R.string.MH_Pin_Accepted))
                Notifications.notify(
                    context,
                    context.getString(R.string.usage_notification_pin_title),
                    context.getString(R.string.usage_notification_pin_text, phoneNumber),
                    Notifications.CHANNEL_PIN
                )

                tempAllowlistRepo.add(phoneNumber, subscriptionId)
                TempContactExpiredService.scheduleJob(context, TEMP_USAGE_VALIDITY_MILLIS + 1000)

                return true
            }
        }

        // Not allowed
        return false
    }

    override fun send(context: Context, msg: String, commandName: String?) {
        super.send(context, msg, commandName)

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val defaultSmsManager = context.getSystemService(SmsManager::class.java)
            if (subscriptionId == -1) {
                defaultSmsManager
            } else {
                defaultSmsManager.createForSubscriptionId(subscriptionId)
            }
        } else {
            if (subscriptionId == -1) {
                SmsManager.getDefault()
            } else {
                SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            }
        }

        if (msg.length <= 160) {
            smsManager.sendTextMessage(phoneNumber, null, msg, null, null)
        } else {
            val parts = smsManager.divideMessage(msg)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        }
    }
}