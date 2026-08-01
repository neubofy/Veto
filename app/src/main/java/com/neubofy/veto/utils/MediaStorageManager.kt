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
        val baseDir = try {
            val externalPublic = File(Environment.getExternalStorageDirectory(), ROOT_DIR_NAME)
            if (!externalPublic.exists()) {
                externalPublic.mkdirs()
            }
            if (externalPublic.canWrite()) externalPublic else context.getExternalFilesDir(null) ?: context.filesDir
        } catch (e: Exception) {
            context.getExternalFilesDir(null) ?: context.filesDir
        }

        val rootDir = File(baseDir, if (baseDir.name == ROOT_DIR_NAME) "" else ROOT_DIR_NAME)
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }

        // Subfolders
        File(rootDir, "Photos").mkdirs()
        File(rootDir, "Videos").mkdirs()
        File(rootDir, "Audio").mkdirs()

        // Ensure .nomedia setting is applied
        updateNoMediaStatus(context, rootDir)

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
        try {
            val settings = SettingsRepository.getInstance(context)
            val hideMedia = settings.get(Settings.SET_HIDE_MEDIA_IN_GALLERY) as? Boolean ?: true
            val targetDirs = listOf(rootDir, getPhotosDir(context), getVideosDir(context), getAudioDir(context))

            for (dir in targetDirs) {
                if (!dir.exists()) dir.mkdirs()
                val noMediaFile = File(dir, ".nomedia")
                if (hideMedia) {
                    if (!noMediaFile.exists()) {
                        noMediaFile.createNewFile()
                        context.log().i(TAG, "Created .nomedia file in ${dir.absolutePath}")
                    }
                } else {
                    if (noMediaFile.exists()) {
                        noMediaFile.delete()
                        context.log().i(TAG, "Deleted .nomedia file from ${dir.absolutePath}")
                    }
                }
            }

            // Trigger MediaScanner scan so gallery updates immediately
            val allMediaFiles = mutableListOf<String>()
            targetDirs.forEach { dir ->
                dir.listFiles()?.filter { it.isFile && it.name != ".nomedia" }?.forEach {
                    allMediaFiles.add(it.absolutePath)
                }
            }
            if (allMediaFiles.isNotEmpty()) {
                android.media.MediaScannerConnection.scanFile(
                    context.applicationContext,
                    allMediaFiles.toTypedArray(),
                    null
                ) { path, uri ->
                    context.log().d(TAG, "MediaScanner scanned $path -> $uri")
                }
            }
        } catch (e: Exception) {
            context.log().e(TAG, "Error updating .nomedia status: ${e.message}")
        }
    }

    fun setupStorage(context: Context): String {
        val root = getRootMediaDir(context)
        getPhotosDir(context)
        getVideosDir(context)
        getAudioDir(context)
        updateNoMediaStatus(context, root)
        return root.absolutePath
    }

    fun openLocalFolder(context: Context) {
        val rootDir = getRootMediaDir(context)
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(android.net.Uri.parse(rootDir.absolutePath), "resource/folder")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                    setDataAndType(android.net.Uri.fromFile(rootDir), "*/*")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, "Veto Storage: ${rootDir.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
}
