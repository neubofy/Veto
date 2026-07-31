package com.neubofy.veto.commands

import android.content.Context
import android.media.MediaRecorder
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.permissions.RecordAudioPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.GoogleDriveUploader
import com.neubofy.veto.utils.log
import java.io.File
import java.io.IOException

class AudioCommand(context: Context) : Command(context) {
    companion object {
        private val TAG = AudioCommand::class.simpleName
    }

    override val keyword = "audio"
    override val usage = "audio"

    @get:DrawableRes
    override val icon = R.drawable.ic_cloud // Using generic cloud icon for now

    @get:StringRes
    override val shortDescription = R.string.cmd_audio_description_short

    override val longDescription = R.string.cmd_audio_description_long

    override val requiredPermissions = listOf(RecordAudioPermission())

    override suspend fun <T> executeInternal(args: List<String>, transport: Transport<T>) {
        if (!settings.serverAccountExists()) {
            transport.send(context, "Cannot record audio: no Veto Server account paired.", keyword)
            return
        }

        // 1. Store locally in cacheDir
        val outputFile = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
        
        val recorder = MediaRecorder()
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.prepare()
            recorder.start()

            context.log().i(TAG, "Started recording audio for 30s...")
            transport.send(context, "Recording 30s of stealth audio...", keyword)

            kotlinx.coroutines.delay(30000L) // Record for 30 seconds

            recorder.stop()
            recorder.release()
            context.log().i(TAG, "Recording complete. Uploading to Drive...")
            
            // 2. Upload to Drive and 3. Clear locally when finished
            GoogleDriveUploader.uploadFile(
                context = context,
                file = outputFile,
                mimeType = "audio/mp4",
                type = "audio",
                onSuccess = { link ->
                    outputFile.delete() // Clear locally
                    transport.send(context, "Audio captured: $link", keyword)
                },
                onError = { error ->
                    outputFile.delete() // Clear locally
                    transport.send(context, "Failed to upload audio: $error", keyword)
                }
            )
        } catch (e: Exception) {
            context.log().e(TAG, "Audio recording failed: ${e.message}")
            transport.send(context, "Audio recording failed: ${e.message}", keyword)
            recorder.release()
            outputFile.delete() // Clear locally
        }
    }
}
