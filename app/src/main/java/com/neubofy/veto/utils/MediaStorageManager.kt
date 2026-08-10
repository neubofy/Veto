package com.neubofy.veto.utils

import android.content.Context
import android.os.Environment
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import java.io.File

object MediaStorageManager {
    private const val TAG = "MediaStorageManager"
    private const val ROOT_DIR_NAME = "Veto"

    fun getRootMediaDir(context: Context): File {
        val baseDir = context.cacheDir
        val rootDir = File(baseDir, ROOT_DIR_NAME)
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }

        // Subfolders
        File(rootDir, "Photos").mkdirs()
        File(rootDir, "Videos").mkdirs()
        File(rootDir, "Audio").mkdirs()

        return rootDir
    }

    fun getPhotosDir(context: Context): File {
        val photosDir = File(getRootMediaDir(context), "Photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        return photosDir
    }

    fun getVideosDir(context: Context): File {
        val videosDir = File(getRootMediaDir(context), "Videos")
        if (!videosDir.exists()) videosDir.mkdirs()
        return videosDir
    }

    fun getAudioDir(context: Context): File {
        val audioDir = File(getRootMediaDir(context), "Audio")
        if (!audioDir.exists()) audioDir.mkdirs()
        return audioDir
    }

    fun updateNoMediaStatus(context: Context, rootDir: File = getRootMediaDir(context)) {
        // Obsolete: cacheDir is already private and ignored by gallery scanners.
    }

    fun verifyPreconditions(context: Context, type: String): String? {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            return "Command failed: Web Dashboard pairing required. Please link device in Dashboard."
        }

        try {
            val rootDir = getRootMediaDir(context)
            if (!rootDir.exists() || !rootDir.canWrite()) {
                return "Command failed: Local storage folder setup incomplete or not writable."
            }
        } catch (e: Exception) {
            return "Command failed: Local storage setup error (${e.message})."
        }



        val prefs = context.getSharedPreferences("veto_drive_prefs", Context.MODE_PRIVATE)
        val folderKey = when (type) {
            "video" -> "drive_folder_video"
            "audio" -> "drive_folder_audio"
            else -> "drive_folder_photo"
        }
        val folderId = prefs.getString(folderKey, null)
        if (folderId.isNullOrBlank()) {
            return "Command failed: Google Drive $type folder not configured. Please complete setup in Dashboard settings."
        }

        return null
    }
}
