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
        commandName: String? = null
    ) {
        syncScope.launch {
            try {
                val now = System.currentTimeMillis()
                val photosDir = MediaStorageManager.getPhotosDir(context)
                val videosDir = MediaStorageManager.getVideosDir(context)
                val audioDir = MediaStorageManager.getAudioDir(context)

                // Photos
                photosDir.listFiles()?.filter { now - it.lastModified() <= maxAgeMillis }?.forEach { file ->
                    uploadMediaFile(context, file, "photo", "image/jpeg", commandName ?: "camera")
                }

                // Videos
                videosDir.listFiles()?.filter { now - it.lastModified() <= maxAgeMillis }?.forEach { file ->
                    uploadMediaFile(context, file, "video", "video/mp4", commandName ?: "video")
                }

                // Audio
                audioDir.listFiles()?.filter { now - it.lastModified() <= maxAgeMillis }?.forEach { file ->
                    uploadMediaFile(context, file, "audio", "audio/mp4", commandName ?: "audio")
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
        commandName: String
    ) {
        val typeCapitalized = type.replaceFirstChar { it.uppercase() }
        GoogleDriveUploader.uploadFile(
            context = context,
            file = file,
            mimeType = mimeType,
            type = type,
            onSuccess = { link ->
                context.log().i(TAG, "$typeCapitalized uploaded successfully to Drive: $link")
                val transport = NextJsServerTransport(context)
                transport.send(
                    context,
                    "$typeCapitalized uploaded to Google Drive: $link",
                    commandName
                )
            },
            onError = { error ->
                context.log().e(TAG, "Failed to upload $type to Drive: $error")
                val transport = NextJsServerTransport(context)
                transport.send(
                    context,
                    "Failed to upload $type to Drive: $error (saved locally in ${file.parentFile?.name})",
                    commandName
                )
            }
        )
    }
}
