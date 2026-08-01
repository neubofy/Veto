package com.neubofy.veto.commands

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.permissions.CameraPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.ui.DummyCameraxActivity
import com.neubofy.veto.utils.log


import com.neubofy.veto.permissions.StoragePermission

class CameraCommand(context: Context) : Command(context) {
    companion object {
        private val TAG = CameraCommand::class.simpleName
    }

    override val keyword = "photo"
    override val usage = "photo [front | back] [flash]"

    @get:DrawableRes
    override val icon = R.drawable.ic_camera

    @get:StringRes
    override val shortDescription = R.string.cmd_camera_description_short

    override val longDescription = R.string.cmd_camera_description_long

    override val requiredPermissions = listOf(CameraPermission(), StoragePermission())

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val error = com.neubofy.veto.utils.MediaStorageManager.verifyPreconditions(context, "photo")
        if (error != null) {
            context.log().w(TAG, error)
            transport.send(context, error, keyword)
            return
        }

        val dummyCameraActivity = Intent(context, DummyCameraxActivity::class.java)
        dummyCameraActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        dummyCameraActivity.putExtra(DummyCameraxActivity.EXTRA_COMMAND, keyword)

        if (args.getOrNull(0) == "front") {
            dummyCameraActivity.putExtra(
                DummyCameraxActivity.EXTRA_CAMERA,
                DummyCameraxActivity.CAMERA_FRONT
            )
        } else {
            dummyCameraActivity.putExtra(
                DummyCameraxActivity.EXTRA_CAMERA,
                DummyCameraxActivity.CAMERA_BACK
            )
        }
        if (args.getOrNull(1) == "flash") {
            dummyCameraActivity.putExtra(DummyCameraxActivity.EXTRA_FLASH, true)
        }
        context.log().d(TAG, "Starting camera activity")
        context.startActivity(dummyCameraActivity)

        transport.send(context, "Capturing photo...", keyword)
    }
}
