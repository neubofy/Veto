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

import com.neubofy.veto.permissions.StoragePermission

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

    override val requiredPermissions = listOf(RecordAudioPermission(), StoragePermission())

    override suspend fun <T> executeInternal(args: List<String>, transport: Transport<T>) {
        val error = com.neubofy.veto.utils.MediaStorageManager.verifyPreconditions(context, "audio")
        if (error != null) {
            transport.send(context, error, keyword)
            return
        }

        val dummyAudioActivity = android.content.Intent(context, com.neubofy.veto.ui.DummyAudioActivity::class.java)
        dummyAudioActivity.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        com.neubofy.veto.transports.TransportHelper.attachTransportToIntent(dummyAudioActivity, transport)
        context.startActivity(dummyAudioActivity)

        transport.send(context, "Capturing 30s audio...", keyword)
    }
}
