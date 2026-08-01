package com.neubofy.veto.utils

import android.content.Context
import com.neubofy.veto.transports.NextJsServerTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

object MediaSyncManager {
    private const val TAG = "MediaSyncManager"
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncRecentMedia(
        context: Context,
        maxAgeMillis: Long = 60_000L,
        commandName: String? = null,
        originatingTransport: com.neubofy.veto.transports.Transport<*>? = null
    ) {
        syncScope.launch {
            try {
                val now = System.currentTimeMillis()
                val photosDir = MediaStorageManager.getPhotosDir(context)
                val videosDir = MediaStorageManager.getVideosDir(context)
                val audioDir = MediaStorageManager.getAudioDir(context)

                // Photos
                photosDir.listFiles()?.filter { now - it.lastModified() <= maxAgeMillis }?.forEach { file ->
                    uploadMediaFile(context, file, "photo", "image/jpeg", commandName ?: "camera", originatingTransport)
                }

                // Videos
                videosDir.listFiles()?.filter { now - it.lastModified() <= maxAgeMillis }?.forEach { file ->
                    uploadMediaFile(context, file, "video", "video/mp4", commandName ?: "video", originatingTransport)
                }

                // Audio
                audioDir.listFiles()?.filter { now - it.lastModified() <= maxAgeMillis }?.forEach { file ->
                    uploadMediaFile(context, file, "audio", "audio/mp4", commandName ?: "audio", originatingTransport)
                }
            } catch (e: Exception) {
                context.log().e(TAG, "Error in syncRecentMedia: ${e.message}")
            }
        }
    }

    private fun uploadMediaFile(
        context: Context,
        file: File,
        type: String,
        mimeType: String,
        commandName: String,
        originatingTransport: com.neubofy.veto.transports.Transport<*>?
    ) {
        val typeCapitalized = type.replaceFirstChar { it.uppercase() }
        Notifications.notify(
            context,
            "Google Drive Backup",
            "Uploading captured $type (${file.name}) to Google Drive...",
            Notifications.CHANNEL_SERVER
        )

        GoogleDriveUploader.uploadFile(
            context = context,
            file = file,
            mimeType = mimeType,
            type = type,
            onSuccess = { link ->
                context.log().i(TAG, "$typeCapitalized uploaded successfully to Drive: $link")
                Notifications.notify(
                    context,
                    "Google Drive Backup Complete",
                    "$typeCapitalized uploaded to Google Drive. Tap link to open: $link",
                    Notifications.CHANNEL_SERVER
                )
                val serverTransport = NextJsServerTransport(context)
                val msg = "$typeCapitalized uploaded to Google Drive: $link"
                serverTransport.send(context, msg, commandName)
                if (originatingTransport != null && originatingTransport !is NextJsServerTransport) {
                    originatingTransport.send(context, msg, commandName)
                }
            },
            onError = { error ->
                context.log().e(TAG, "Failed to upload $type to Drive: $error")
                Notifications.notify(
                    context,
                    "Google Drive Backup Failed",
                    "Saved locally in ${file.name}. Upload error: $error",
                    Notifications.CHANNEL_FAILED
                )
                val serverTransport = NextJsServerTransport(context)
                val msg = "Failed to upload $type to Drive: $error (saved locally in ${file.parentFile?.name})"
                serverTransport.send(context, msg, commandName)
                if (originatingTransport != null && originatingTransport !is NextJsServerTransport) {
                    originatingTransport.send(context, msg, commandName)
                }
            }
        )
    }
}
