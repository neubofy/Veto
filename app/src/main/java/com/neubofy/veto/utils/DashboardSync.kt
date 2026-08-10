package com.neubofy.veto.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object DashboardSync {
    private val TAG = DashboardSync::class.java.simpleName

    fun uploadTokenIfPaired(context: Context, retryCount: Int = 0, callback: ((String, Boolean) -> Unit)? = null) {
        val settings = SettingsRepository.getInstance(context)
        val dashboardUrl = settings.get(Settings.SET_VetoSERVER_URL) as String
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (dashboardUrl.isEmpty() || currentUser == null) {
            context.log().i(TAG, "Dashboard not paired or user not authenticated. Skipping token upload.")
            callback?.invoke("Dashboard not paired or user not authenticated.", false)
            return
        }
        if (currentUser == null) {
            context.log().e(TAG, "User not authenticated. Cannot sync token.")
            if (retryCount < 3) {
                context.log().i(TAG, "Retrying token sync in 10 seconds (attempt ${retryCount + 1})...")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    uploadTokenIfPaired(context, retryCount + 1, callback)
                }, 10000)
            } else {
                callback?.invoke("User not authenticated.", false)
            }
            return
        }

        callback?.invoke("Fetching Firebase Auth token...", true)
        currentUser.getIdToken(true).addOnCompleteListener { task ->
            if (!task.isSuccessful || task.result == null) {
                context.log().e(TAG, "Failed to get Firebase Auth token: ${task.exception?.message}")
                callback?.invoke("Auth Token Error: ${task.exception?.message}", false)
                return@addOnCompleteListener
            }

            val idToken = task.result?.token ?: return@addOnCompleteListener

            callback?.invoke("Fetching FCM token...", true)
            FirebaseMessaging.getInstance().token.addOnCompleteListener { fcmTask ->
                if (!fcmTask.isSuccessful) {
                    context.log().w(TAG, "Fetching FCM registration token failed: ${fcmTask.exception?.message}")
                    callback?.invoke("FCM Token Error: ${fcmTask.exception?.message}", false)
                    return@addOnCompleteListener
                }

                val fcmToken = fcmTask.result

                Thread {
                    try {
                        callback?.invoke("Syncing to server...", true)
                        val apiUrl = if (dashboardUrl.endsWith("/")) "${dashboardUrl}api/device/link" else "$dashboardUrl/api/device/link"
                        val url = URL(apiUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.setRequestProperty("Authorization", "Bearer $idToken")
                        connection.doOutput = true

                        val jsonParam = JSONObject()
                        jsonParam.put("fcmToken", fcmToken)

                        val out = OutputStreamWriter(connection.outputStream)
                        out.write(jsonParam.toString())
                        out.close()

                        val responseCode = connection.responseCode
                        if (responseCode in 200..299) {
                            context.log().i(TAG, "Successfully synced token to Dashboard")
                            settings.set(Settings.SET_SYNCED_FCM_TOKEN, fcmToken)
                            callback?.invoke("Synced Successfully!", true)
                        } else {
                            context.log().e(TAG, "Failed to sync token. Server returned $responseCode")
                            callback?.invoke("Server Error: $responseCode", false)
                        }
                    } catch (e: Exception) {
                        context.log().e(TAG, "Error syncing token: ${e.message}")
                        callback?.invoke("Network Error: ${e.message}", false)
                    }
                }.start()
            }
        }
    }
}
