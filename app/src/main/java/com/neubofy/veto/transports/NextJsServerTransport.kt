package com.neubofy.veto.transports

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.neubofy.veto.R
import com.neubofy.veto.commands.ParserResult
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

    companion object {
        fun isConnected(context: Context): Boolean {
            val settings = SettingsRepository.getInstance(context)
            val dashboardUrl = settings.get(Settings.SET_VetoSERVER_URL) as String
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (dashboardUrl.isEmpty() || currentUser == null) return false
            return true
        }
    }

    override val icon = R.drawable.ic_in_app
    override val title = R.string.transport_inapp_title // Reuse title or create new
    override val description = "Sends command results back to the Dashboard"
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
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser

        if (dashboardUrl.isEmpty() || currentUser == null) {
            context.log().i("NextJsServerTransport", "Dashboard not paired. Skipping result upload.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Using Tasks.await prevents thread deadlocks compared to the old CountDownLatch
                val tokenResult = Tasks.await(currentUser.getIdToken(false), 30, TimeUnit.SECONDS)
                val idToken = tokenResult?.token
                if (idToken == null) {
                    context.log().e("NextJsServerTransport", "Firebase Auth token is null")
                    return@launch
                }

                val apiUrl = if (dashboardUrl.endsWith("/")) "${dashboardUrl}api/command/result" else "$dashboardUrl/api/command/result"
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $idToken")
                connection.doOutput = true

                val jsonParam = JSONObject()
                
                val encRepo = com.neubofy.veto.data.EncryptedSettingsRepository.getInstance(context)
                val rawPin = encRepo.getRawVetoPin()
                val uid = currentUser.uid
                
                if (rawPin.isNullOrBlank()) {
                    context.log().e("NextJsServerTransport", "Encryption aborted: Veto PIN is not configured on device.")
                    throw IllegalStateException("Veto PIN required for zero-knowledge encrypted telemetry storage.")
                }

                val encryptedMsg = try {
                    com.neubofy.veto.utils.VetoCrypto.encrypt(msg, rawPin, uid)
                } catch (e: Exception) {
                    context.log().e("NextJsServerTransport", "Encryption failed: ${e.message}")
                    throw IllegalStateException("Failed to encrypt telemetry output: ${e.message}")
                }

                jsonParam.put("result", encryptedMsg)
                jsonParam.put("encrypted", true)

                if (commandName != null) {
                    jsonParam.put("command", commandName)
                }

                val out = OutputStreamWriter(connection.outputStream)
                out.write(jsonParam.toString())
                out.close()

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    context.log().i("NextJsServerTransport", "Successfully synced command result to Dashboard")
                } else {
                    context.log().e("NextJsServerTransport", "Failed to sync result. Server returned $responseCode")
                    fallbackToSms(context, msg, commandName)
                }
            } catch (e: ExecutionException) {
                context.log().e("NextJsServerTransport", "ExecutionException: ${e.message}")
                fallbackToSms(context, msg, commandName)
            } catch (e: InterruptedException) {
                context.log().e("NextJsServerTransport", "InterruptedException: ${e.message}")
                fallbackToSms(context, msg, commandName)
            } catch (e: TimeoutException) {
                context.log().e("NextJsServerTransport", "TimeoutException: ${e.message}")
                fallbackToSms(context, msg, commandName)
            } catch (e: Exception) {
                context.log().e("NextJsServerTransport", "Error syncing result: ${e.message}")
                fallbackToSms(context, msg, commandName)
            }
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

        // The actual encryption happens inside send(), so we just pass the JSON string here.
        // Wait, send() takes the msg and wraps it into {"result": msg}.
        // If msg is already JSON string of location, Next.js server will receive it as a string inside "result".
        // That's what it did before.
        send(context, json.toString(), commandName)
    }
}
