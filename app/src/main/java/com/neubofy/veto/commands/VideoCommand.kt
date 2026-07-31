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

import com.neubofy.veto.permissions.StoragePermission

class VideoCommand(context: Context) : Command(context) {
    override val keyword = "video"
    override val usage = "video [front | back]"

    @get:DrawableRes
    override val icon = R.drawable.ic_camera

    @get:StringRes
    override val shortDescription = R.string.cmd_video_description_short

    override val longDescription = R.string.cmd_video_description_long

    override val requiredPermissions = listOf(CameraPermission(), RecordAudioPermission(), StoragePermission())

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        if (!settings.serverAccountExists()) {
            transport.send(context, "Cannot record video: no Veto Server account paired.", keyword)
            return
        }

        val dummyCameraActivity = Intent(context, DummyCameraxActivity::class.java)
        dummyCameraActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        dummyCameraActivity.putExtra(DummyCameraxActivity.EXTRA_COMMAND, keyword)

        if (args.getOrNull(0) == "front") {
            dummyCameraActivity.putExtra(DummyCameraxActivity.EXTRA_CAMERA, DummyCameraxActivity.CAMERA_FRONT)
        } else {
            dummyCameraActivity.putExtra(DummyCameraxActivity.EXTRA_CAMERA, DummyCameraxActivity.CAMERA_BACK)
        }
        
        context.startActivity(dummyCameraActivity)
        transport.send(context, "Capturing 30s video...", keyword)
    }
}
