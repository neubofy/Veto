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
        val userId = settings.get(Settings.SET_VetoSERVER_ID) as String

        if (dashboardUrl.isEmpty() || userId.isEmpty()) {
            context.log().i("NextJsServerTransport", "Dashboard not paired. Skipping result upload.")
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            context.log().e("NextJsServerTransport", "User not authenticated. Cannot sync result.")
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
                // Send the raw string. The Next.js server will parse it if it is JSON.
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
                } else {
                    context.log().e("NextJsServerTransport", "Failed to sync result. Server returned $responseCode")
                }
            } catch (e: ExecutionException) {
                context.log().e("NextJsServerTransport", "ExecutionException: ${e.message}")
            } catch (e: InterruptedException) {
                context.log().e("NextJsServerTransport", "InterruptedException: ${e.message}")
            } catch (e: TimeoutException) {
                context.log().e("NextJsServerTransport", "TimeoutException: ${e.message}")
            } catch (e: Exception) {
                context.log().e("NextJsServerTransport", "Error syncing result: ${e.message}")
            }
        }
    }

    override fun sendNewLocation(context: Context, location: com.neubofy.veto.data.VetoLocation, commandName: String?) {
        val json = JSONObject()
        json.put("type", "location")
        json.put("lat", location.lat)
        json.put("lon", location.lon)
        json.put("provider", location.provider)
        json.put("accuracy", "${location.accuracy}m")
        json.put("batteryLevel", location.batteryLevel)
        json.put("timeMillis", location.timeMillis)
        if (location.altitude != null) json.put("altitude", location.altitude)
        if (location.bearing != null) json.put("bearing", location.bearing)
        if (location.speed != null) json.put("speed", location.speed)

        // Send structured JSON instead of string
        send(context, json.toString(), commandName)
    }
}
