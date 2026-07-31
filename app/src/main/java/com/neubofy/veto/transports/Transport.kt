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
