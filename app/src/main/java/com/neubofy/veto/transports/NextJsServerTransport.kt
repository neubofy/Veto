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

        try {
            // Using Tasks.await prevents thread deadlocks compared to the old CountDownLatch
            val tokenResult = Tasks.await(currentUser.getIdToken(false), 30, TimeUnit.SECONDS)
            val idToken = tokenResult?.token
            if (idToken == null) {
                context.log().e("NextJsServerTransport", "Firebase Auth token is null")
                return
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

    override fun sendNewLocation(context: Context, location: com.neubofy.veto.data.VetoLocation, commandName: String?) {
        val settings = SettingsRepository.getInstance(context)

        // Only store to autoloc recent cache when this IS autoloc — don't pollute with locate data
        if (commandName == "autoloc") {
            settings.storeRecentLocation(location)
        }

        val isAutoLoc = commandName == "autoloc"
        var shouldUpload = false

        if (!isAutoLoc) {
            // Manual locate command: ALWAYS upload immediately!
            shouldUpload = true
        } else {
            // Background autoloc tracking: upload if first time or movement > 100m
            val lastUploaded = settings.getLastUploadedLocation()
            if (lastUploaded == null) {
                shouldUpload = true
            } else {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    lastUploaded.lat, lastUploaded.lon,
                    location.lat, location.lon,
                    results
                )
                val distanceMeters = results[0]
                if (distanceMeters > 100f) {
                    shouldUpload = true
                    context.log().i("NextJsServerTransport", "AutoLoc distance $distanceMeters m > 100m. Uploading to Dashboard.")
                } else {
                    context.log().i("NextJsServerTransport", "AutoLoc distance $distanceMeters m <= 100m. Skipping periodic upload.")
                }
            }
        }

        if (shouldUpload) {
            if (isAutoLoc) {
                settings.setLastUploadedLocation(location)
            }
            
            val json = JSONObject()
            json.put("type", "location")
            json.put("lat", location.lat)
            json.put("lon", location.lon)
            json.put("provider", location.provider)
            json.put("accuracy", "${location.accuracy}m")

            // Send structured JSON instead of string
            super.send(context, json.toString(), commandName)
        }
    }
}
