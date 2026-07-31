package com.neubofy.veto.transports

import android.content.Context
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
        return true
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

        val latch = java.util.concurrent.CountDownLatch(1)

        currentUser.getIdToken(false).addOnCompleteListener { task ->
            if (!task.isSuccessful || task.result == null) {
                context.log().e("NextJsServerTransport", "Failed to get Firebase Auth token: ${task.exception?.message}")
                latch.countDown()
                return@addOnCompleteListener
            }

            val idToken = task.result?.token
            if (idToken == null) {
                latch.countDown()
                return@addOnCompleteListener
            }

            Thread {
                try {
                    val apiUrl = if (dashboardUrl.endsWith("/")) "${dashboardUrl}api/command/result" else "$dashboardUrl/api/command/result"
                    val url = URL(apiUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", "Bearer $idToken")
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
                    } else {
                        context.log().e("NextJsServerTransport", "Failed to sync result. Server returned $responseCode")
                    }
                } catch (e: Exception) {
                    context.log().e("NextJsServerTransport", "Error syncing result: ${e.message}")
                } finally {
                    latch.countDown()
                }
            }.start()
        }

        try {
            latch.await(30, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            context.log().e("NextJsServerTransport", "Interrupted while waiting for sync")
        }
    }
}
