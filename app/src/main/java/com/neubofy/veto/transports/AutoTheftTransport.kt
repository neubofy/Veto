package com.neubofy.veto.transports

import android.content.Context
import android.telephony.SubscriptionManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.commands.ParserResult
import com.neubofy.veto.data.AllowlistRepository
import com.neubofy.veto.utils.NetworkUtils
import com.neubofy.veto.utils.log

/**
 * Unified Smart Transport used by Auto-Theft commands:
 * 1. Tries NextJsServerTransport if internet is active.
 * 2. Fallbacks to sending SMS to Starred Allowed Contacts if cellular network is available.
 * 3. Media captures automatically save to local Downloads folder via MediaStorageManager.
 */
class AutoTheftTransport(
    private val context: Context
) : Transport<Unit>(Unit) {

    companion object {
        private val TAG = AutoTheftTransport::class.simpleName
    }

    @get:DrawableRes
    override val icon = R.drawable.ic_security

    @get:StringRes
    override val title = R.string.command_theft_description

    override val description = "Smart Auto-Theft Multi-Channel Transport"
    override val requiredPermissions = emptyList<com.neubofy.veto.permissions.Permission>()
    override val actions = emptyList<TransportAction>()

    override fun getDestinationString(): String = "AutoTheftSmartTransport"

    override fun isAllowed(parsed: ParserResult.Success): Boolean = true

    override fun send(context: Context, msg: String, commandName: String?) {
        super.send(context, msg, commandName)

        val ips = NetworkUtils.getIps(context)
        val hasInternet = ips.isNotEmpty()

        if (hasInternet) {
            try {
                val serverTransport = NextJsServerTransport(context)
                serverTransport.send(context, msg, commandName)
                context.log().i(TAG, "Successfully sent Auto-Theft payload to server ($commandName)")
                return
            } catch (e: Exception) {
                context.log().w(TAG, "Server transport failed, trying SMS fallback: ${e.message}")
            }
        } else {
            context.log().i(TAG, "Internet offline. Trying Starred Contact SMS fallback...")
        }

        // Secondary Fallback: SMS to Starred Contacts
        try {
            val allowlistRepo = AllowlistRepository.getInstance(context)
            val starredContacts = allowlistRepo.getStarredContacts()

            if (starredContacts.isNotEmpty()) {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                var subId = -1
                try {
                    @Suppress("MissingPermission")
                    val activeSubs = subManager?.activeSubscriptionInfoList
                    if (!activeSubs.isNullOrEmpty()) {
                        subId = activeSubs[0].subscriptionId
                    }
                } catch (_: Exception) {}

                for (contact in starredContacts) {
                    val smsTransport = SmsTransport(context, contact.number, subId)
                    smsTransport.send(context, "🚨 [Veto Auto-Theft] $msg", commandName)
                    context.log().i(TAG, "Sent emergency SMS to starred contact ${contact.name}")
                }
            } else {
                context.log().w(TAG, "No starred contacts configured for offline SMS exfiltration.")
            }
        } catch (e: Exception) {
            context.log().e(TAG, "Failed SMS fallback: ${e.message}")
        }
    }
}
