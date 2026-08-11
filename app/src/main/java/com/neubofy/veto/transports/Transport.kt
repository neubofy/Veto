package com.neubofy.veto.transports

import android.content.Context
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.commands.ParserResult
import com.neubofy.veto.data.VetoLocation
import com.neubofy.veto.permissions.Permission
import com.neubofy.veto.utils.log


// Order matters for the home screen
fun availableTransports(context: Context): List<Transport<*>> = listOf(
    SmsTransport(context, "42", -1),
    NotificationReplyTransport(context, null),
    InAppTransport(context),
)


abstract class Transport<DestinationType>(
    private val destination: DestinationType
) {
    companion object {
        private val TAG = Transport::class.simpleName
    }

    @get:DrawableRes
    abstract val icon: Int

    @get:StringRes
    abstract val title: Int

    abstract val description: String

    open val descriptionAuth: String? = null

    open val descriptionNote: String? = null

    abstract val requiredPermissions: List<Permission>

    open val actions: List<TransportAction> = emptyList()

    fun missingRequiredPermissions(context: Context): List<Permission> {
        return requiredPermissions.filter { p -> !p.isGranted(context) }
    }

    abstract fun getDestinationString(): String

    /**
     * Whether this transport instance is allowed to execute the command from the [ParserResult].
     */
    abstract fun isAllowed(parsed: ParserResult.Success): Boolean

    /**
     * Sends the text back to the remote user via this transport.
     * The original string might be too long so it gets truncated and chunked.
     * @param commandName the name of the command that triggered this send, if available.
     */
    @CallSuper
    open fun send(context: Context, msg: String, commandName: String? = null) {
        val missing = missingRequiredPermissions(context)
        if (missing.isNotEmpty()) {
            context.log()
                .w(TAG, "Cannot send message: missing permissions ${missing.joinToString(", ")}")
            return
        }
    }

    protected fun fallbackToSms(context: Context, msg: String, commandName: String?) {
        val allowlistRepo = com.neubofy.veto.data.AllowlistRepository.getInstance(context)
        val starredContact = allowlistRepo.list.find { it.isStarred }
        if (starredContact == null) {
            context.log().w(TAG, "Fallback SMS aborted: No starred contact found.")
            return
        }

        context.log().i(TAG, "Triggering fallback SMS to starred contact: ${starredContact.name}")

        val sm = context.getSystemService(android.telephony.SubscriptionManager::class.java)
        
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            val activeSubscriptionInfoList = sm?.activeSubscriptionInfoList
            if (!activeSubscriptionInfoList.isNullOrEmpty()) {
                // To guarantee delivery, we iterate through all active SIMs and attempt to send.
                for (subInfo in activeSubscriptionInfoList) {
                    try {
                        val smsTransport = SmsTransport(context, starredContact.number, subInfo.subscriptionId)
                        val fallbackMsg = "[Fallback] " + msg
                        smsTransport.send(context, fallbackMsg, commandName)
                        context.log().i(TAG, "Fallback SMS sent via SIM: ${subInfo.subscriptionId}")
                    } catch (e: Exception) {
                        context.log().e(TAG, "Fallback SMS failed for SIM ${subInfo.subscriptionId}: ${e.message}")
                    }
                }
                return
            }
        }
        
        // If no READ_PHONE_STATE permission or no active subs found, try default SmsManager
        try {
            val smsTransport = SmsTransport(context, starredContact.number, -1)
            val fallbackMsg = "[Fallback] " + msg
            smsTransport.send(context, fallbackMsg, commandName)
            context.log().i(TAG, "Fallback SMS sent via default SIM.")
        } catch (e: Exception) {
            context.log().e(TAG, "Fallback SMS via default SIM failed: ${e.message}")
        }
    }

    open fun sendNewLocation(context: Context, location: VetoLocation, commandName: String? = null) {
        send(context, location.toString(), commandName)
    }

    /**
     * Closes the transport channel
     */
    open fun closeChannel() {
        // nothing to do, but may be overridden
    }
}
