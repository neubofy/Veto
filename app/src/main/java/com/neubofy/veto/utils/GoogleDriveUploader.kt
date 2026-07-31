package com.neubofy.veto.utils

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files

object GoogleDriveUploader {

    private const val TAG = "GoogleDriveUploader"
    private const val DRIVE_API_URL = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"

    private fun getGso(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
    }

    /**
     * Obtains a fresh token just-in-time synchronously.
     * Must be called from a background thread.
     */
    private fun getValidToken(context: Context): String {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val accountObj = account?.account ?: throw Exception("No Google account found. Please sign in.")
        val scope = "oauth2:https://www.googleapis.com/auth/drive.file"
        return com.google.android.gms.auth.GoogleAuthUtil.getToken(context, accountObj, scope)
    }

    fun setupDrive(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                val token = getValidToken(context)
                val settings = SettingsRepository.getInstance(context)
                    
                    // 1. Find or create root 'Veto' folder
                    var rootId = findFolder(token, "Veto", "root")
                    if (rootId == null) {
                        rootId = createFolder(token, "Veto", "root")
                    }
                    if (rootId == null) throw Exception("Failed to setup root Veto folder")
                    
                    // 2. Find or create subfolders
                    val videoId = findFolder(token, "veto video", rootId) ?: createFolder(token, "veto video", rootId)
                    val photoId = findFolder(token, "veto photo", rootId) ?: createFolder(token, "veto photo", rootId)
                    val audioId = findFolder(token, "veto audio", rootId) ?: createFolder(token, "veto audio", rootId)
                    
                    if (videoId == null || photoId == null || audioId == null) {
                        throw Exception("Failed to setup subfolders")
                    }

                    val prefs = context.getSharedPreferences("veto_drive_prefs", Context.MODE_PRIVATE)
                    prefs.edit().apply {
                        putString("drive_folder_video", videoId)
                        putString("drive_folder_photo", photoId)
                        putString("drive_folder_audio", audioId)
                        apply()
                    }

                    context.log().i(TAG, "Google Drive setup complete")
                onSuccess()
            } catch (e: Exception) {
                context.log().e(TAG, "Setup failed: ${e.message}")
                onError(e.message ?: "Unknown error during setup")
            }
        }.start()
    }

    fun uploadFile(context: Context, file: File, mimeType: String, type: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                val token = getValidToken(context)
                val prefs = context.getSharedPreferences("veto_drive_prefs", Context.MODE_PRIVATE)
                    val folderId = when (type) {
                        "video" -> prefs.getString("drive_folder_video", null)
                        "audio" -> prefs.getString("drive_folder_audio", null)
                        else -> prefs.getString("drive_folder_photo", null)
                    }

                    if (folderId == null) {
                        onError("Drive folder not setup. Please setup Drive in Account Settings.")
                        return@Thread
                    }

                    val boundary = "==BOUNDARY=="
                    val url = URL(DRIVE_UPLOAD_URL)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 30000 // 30 seconds
                    connection.readTimeout = 60000    // 60 seconds
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                    connection.doOutput = true

                    val outputStream = connection.outputStream
                    val writer = PrintWriter(OutputStreamWriter(outputStream, "UTF-8"), true)

                    // Metadata part
                    writer.append("--$boundary\r\n")
                    writer.append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    val metadata = JSONObject()
                    metadata.put("name", file.name)
                    metadata.put("parents", org.json.JSONArray().put(folderId))
                    writer.append(metadata.toString()).append("\r\n")

                    // File part
                    writer.append("--$boundary\r\n")
                    writer.append("Content-Type: $mimeType\r\n\r\n")
                    writer.flush()

                    Files.copy(file.toPath(), outputStream)
                    outputStream.flush()

                    writer.append("\r\n--$boundary--\r\n")
                    writer.flush()
                    writer.close()

                    if (connection.responseCode in 200..299) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(response)
                        val fileId = json.getString("id")
                        
                        // We need the webViewLink, which requires a follow-up GET request
                        val linkUrl = URL("$DRIVE_API_URL/$fileId?fields=webViewLink")
                        val linkConn = linkUrl.openConnection() as HttpURLConnection
                        linkConn.connectTimeout = 15000
                        linkConn.readTimeout = 15000
                        linkConn.setRequestProperty("Authorization", "Bearer $token")
                        if (linkConn.responseCode in 200..299) {
                            val linkRes = linkConn.inputStream.bufferedReader().use { it.readText() }
                            val linkJson = JSONObject(linkRes)
                            onSuccess(linkJson.getString("webViewLink"))
                        } else {
                            onSuccess("https://drive.google.com/file/d/$fileId/view") // Fallback
                        }
                    } else {
                        val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        onError("Upload failed: ${connection.responseCode} $errorStream")
                    }
            } catch (e: Exception) {
                onError("Upload exception: ${e.message}")
            }
        }.start()
    }

    private fun findFolder(token: String, name: String, parentId: String): String? {
        val query = "mimeType='application/vnd.google-apps.folder' and name='$name' and '$parentId' in parents and trashed=false"
        val url = URL("$DRIVE_API_URL?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id)")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.setRequestProperty("Authorization", "Bearer $token")
        
        if (connection.responseCode in 200..299) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val files = json.getJSONArray("files")
            if (files.length() > 0) {
                return files.getJSONObject(0).getString("id")
            }
        }
        return null
    }

    private fun createFolder(token: String, name: String, parentId: String): String? {
        val url = URL(DRIVE_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val json = JSONObject()
        json.put("name", name)
        json.put("mimeType", "application/vnd.google-apps.folder")
        if (parentId != "root") {
            json.put("parents", org.json.JSONArray().put(parentId))
        }

        connection.outputStream.writer().use { it.write(json.toString()) }

        if (connection.responseCode in 200..299) {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val resJson = JSONObject(response)
            return resJson.getString("id")
        }
        return null
    }
}
