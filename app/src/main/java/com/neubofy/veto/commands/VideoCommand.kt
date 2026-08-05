package com.neubofy.veto.commands

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.permissions.CameraPermission
import com.neubofy.veto.permissions.RecordAudioPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.ui.DummyCameraxActivity
import com.neubofy.veto.utils.log



class VideoCommand(context: Context) : Command(context) {
    override val keyword = "video"
    override val usage = "video [front | back] [flash]"

    @get:DrawableRes
    override val icon = R.drawable.ic_camera

    @get:StringRes
    override val shortDescription = R.string.cmd_video_description_short

    override val longDescription = R.string.cmd_video_description_long

    override val requiredPermissions = listOf(CameraPermission(), RecordAudioPermission())

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val error = com.neubofy.veto.utils.MediaStorageManager.verifyPreconditions(context, "video")
        if (error != null) {
            transport.send(context, error, keyword)
            return
        }

        val dummyCameraActivity = Intent(context, DummyCameraxActivity::class.java)
        dummyCameraActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        dummyCameraActivity.putExtra(DummyCameraxActivity.EXTRA_COMMAND, keyword)
        com.neubofy.veto.transports.TransportHelper.attachTransportToIntent(dummyCameraActivity, transport)

        if (args.contains("front")) {
            dummyCameraActivity.putExtra(DummyCameraxActivity.EXTRA_CAMERA, DummyCameraxActivity.CAMERA_FRONT)
        } else {
            dummyCameraActivity.putExtra(DummyCameraxActivity.EXTRA_CAMERA, DummyCameraxActivity.CAMERA_BACK)
        }

        if (args.contains("flash")) {
            dummyCameraActivity.putExtra(DummyCameraxActivity.EXTRA_FLASH, true)
        }
        
        context.startActivity(dummyCameraActivity)
        transport.send(context, "Capturing 30s video...", keyword)
    }
}
