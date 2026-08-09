package com.neubofy.veto.transports

import android.content.Context
import android.telephony.SubscriptionManager
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.neubofy.veto.R
import com.neubofy.veto.commands.ParserResult
import com.neubofy.veto.data.AllowlistRepository
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.utils.log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NextJsServerTransport(
    private val context: Context,
) : Transport<Unit>(Unit) {

    override val icon = R.drawable.ic_in_app
    override val title = R.string.transport_inapp_title
    override val description = "Sends command results back to the Dashboard with Starred Contact SMS Fallback"
    override val requiredPermissions = emptyList<com.neubofy.veto.permissions.Permission>()
    override val actions = emptyList<TransportAction>()

    override fun getDestinationString(): String = "Next.js Dashboard"

    override fun isAllowed(parsed: ParserResult.Success): Boolean {
        val encRepo = com.neubofy.veto.data.EncryptedSettingsRepository.getInstance(context)
        return encRepo.isTransportEnabled("cloud")
    }

    override fun send(context: Context, msg: String, commandName: String?) {
        super.send(context, msg, commandName)

        val settings = SettingsRepository.getInstance(context)
        val dashboardUrl = settings.get(Settings.SET_VetoSERVER_URL) as String
        val userId = settings.get(Settings.SET_VetoSERVER_ID) as String

        val currentUser = FirebaseAuth.getInstance().currentUser

        CoroutineScope(Dispatchers.IO).launch {
            var serverSyncSuccess = false

            if (dashboardUrl.isNotEmpty() && userId.isNotEmpty() && currentUser != null) {
                try {
                    val tokenResult = Tasks.await(currentUser.getIdToken(false), 15, TimeUnit.SECONDS)
                    val idToken = tokenResult?.token

                    if (idToken != null) {
                        val apiUrl = if (dashboardUrl.endsWith("/")) "${dashboardUrl}api/command/result" else "$dashboardUrl/api/command/result"
                        val url = URL(apiUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.setRequestProperty("Authorization", "Bearer $idToken")
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        connection.doOutput = true

                        val jsonParam = JSONObject()
                        jsonParam.put("result", msg)
                        if (commandName != null) {
                            jsonParam.put("command", commandName)
                        }

                        val out = OutputStreamWriter(connection.outputStream)
                        out.write(jsonParam.toString())
                        out.close()

                        val responseCode = connection.responseCode
                        if (responseCode in 200..299) {
                            context.log().i("NextJsServerTransport", "Successfully synced command result to Dashboard")
                            serverSyncSuccess = true
                        } else {
                            context.log().e("NextJsServerTransport", "Failed to sync result. Server returned $responseCode")
                        }
                    }
                } catch (e: Exception) {
                    context.log().e("NextJsServerTransport", "Server upload failed/offline: ${e.message}")
                }
            }

            // Universal Fallback: If server sync failed or offline, fallback to SMS to Starred Contacts
            if (!serverSyncSuccess) {
                fallbackToStarredSms(context, msg, commandName)
            }
        }
    }

    private fun fallbackToStarredSms(context: Context, msg: String, commandName: String?) {
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

                val shortCmd = commandName ?: "cmd"
                val smsText = "[Veto $shortCmd] $msg"

                for (contact in starredContacts) {
                    val smsTransport = SmsTransport(context, contact.number, subId)
                    smsTransport.send(context, smsText, commandName)
                    context.log().i("NextJsServerTransport", "Fallback SMS sent to starred contact ${contact.name}")
                }
            } else {
                context.log().i("NextJsServerTransport", "Server offline and no Starred Contacts set for SMS fallback.")
            }
        } catch (e: Exception) {
            context.log().e("NextJsServerTransport", "Failed to send fallback SMS: ${e.message}")
        }
    }

    override fun sendNewLocation(context: Context, location: com.neubofy.veto.data.VetoLocation, commandName: String?) {
        val json = JSONObject()
        json.put("type", "location")
        json.put("lat", location.lat)
        json.put("lon", location.lon)
        json.put("provider", location.provider)
        json.put("accuracy", if (location.accuracy != null) "${location.accuracy}m" else "N/A")
        json.put("battery", "${location.batteryLevel}%")
        json.put("batteryLevel", location.batteryLevel)
        if (location.speed != null) json.put("speed", "${(location.speed * 3.6).toInt()} km/h")
        if (location.altitude != null) json.put("altitude", "${location.altitude.toInt()}m")
        json.put("timestamp", java.util.Date(location.timeMillis).toString())

        // Also add Google Maps URL for easy SMS reading
        val mapsUrl = "https://maps.google.com/?q=${location.lat},${location.lon}"
        json.put("mapsUrl", mapsUrl)

        send(context, json.toString(), commandName)
    }
}
