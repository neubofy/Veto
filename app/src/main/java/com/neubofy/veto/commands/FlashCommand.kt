package com.neubofy.veto.commands

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.permissions.Permission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.log
import kotlinx.coroutines.delay


class FlashCommand(context: Context) : Command(context) {

    companion object {
        private val TAG = FlashCommand::class.simpleName
    }

    override val keyword = "flash"
    override val usage = "flash [on | off]"

    @get:DrawableRes
    override val icon = R.drawable.ic_flashlight_on

    @get:StringRes
    override val shortDescription = R.string.cmd_flash_description_short

    // Testing on GrapheneOS (Android 16) showed that flashing works without needing the camera permission.
    override val requiredPermissions = emptyList<Permission>()

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val cameraManager = context.getSystemService(CameraManager::class.java)
        var cameraId: String? = null
        var torchStrength = 1

        // Find a suitable camera with flash
        for (id in cameraManager.cameraIdList) {
            val chars = cameraManager.getCameraCharacteristics(id)
            val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

            if (hasFlash) {
                cameraId = id

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    val torchMax =
                        chars.get(CameraCharacteristics.FLASH_TORCH_STRENGTH_MAX_LEVEL) ?: 1
                    // 1 indicates that the feature is not supported
                    if (torchMax > 1) {
                        // Lower the strength. On a Pixel 8, even 30% is still very bright.
                        torchStrength = torchMax / 3
                    }
//                    context.log().d(TAG, "torchMax=$torchMax torchStrength=$torchStrength")
                }
                break
            }
        }

        if (cameraId == null) {
            context.log().w(TAG, "Cannot flash: no camera has a torch")
            return
        }

        if (args.contains("on")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && torchStrength != 1) {
                cameraManager.turnOnTorchWithStrengthLevel(cameraId, torchStrength)
            } else {
                cameraManager.setTorchMode(cameraId, true)
            }
            transport.send(context, "Flashlight turned ON", keyword)
        } else if (args.contains("off")) {
            cameraManager.setTorchMode(cameraId, false)
            transport.send(context, "Flashlight turned OFF", keyword)
        } else {
            // Flash the camera for a few seconds
            for (i in 1..10) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && torchStrength != 1) {
                    cameraManager.turnOnTorchWithStrengthLevel(cameraId, torchStrength)
                } else {
                    cameraManager.setTorchMode(cameraId, true)
                }
                delay(500)
                cameraManager.setTorchMode(cameraId, false)
                delay(500)
            }
            transport.send(context, "Flashlight blinked", keyword)
        }
    }
}
