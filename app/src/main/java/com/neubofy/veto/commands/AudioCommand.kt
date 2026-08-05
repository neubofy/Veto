package com.neubofy.veto.commands

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.permissions.RecordAudioPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.log


class AudioCommand(context: Context) : Command(context) {
    companion object {
        private val TAG = AudioCommand::class.simpleName
        const val EXTRA_DURATION_SECS = "EXTRA_DURATION_SECS"
    }

    override val keyword = "audio"
    override val usage = "audio [duration_secs]"

    @get:DrawableRes
    override val icon = R.drawable.ic_cloud

    @get:StringRes
    override val shortDescription = R.string.cmd_audio_description_short

    override val longDescription = R.string.cmd_audio_description_long

    override val requiredPermissions = listOf(RecordAudioPermission())

    override suspend fun <T> executeInternal(args: List<String>, transport: Transport<T>) {
        val error = com.neubofy.veto.utils.MediaStorageManager.verifyPreconditions(context, "audio")
        if (error != null) {
            transport.send(context, error, keyword)
            return
        }

        val durationSecs = args.getOrNull(0)?.toLongOrNull()?.coerceIn(5L, 300L) ?: 30L

        val dummyAudioActivity = android.content.Intent(context, com.neubofy.veto.ui.DummyAudioActivity::class.java)
        dummyAudioActivity.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        dummyAudioActivity.putExtra(EXTRA_DURATION_SECS, durationSecs)
        com.neubofy.veto.transports.TransportHelper.attachTransportToIntent(dummyAudioActivity, transport)
        context.startActivity(dummyAudioActivity)

        transport.send(context, "Capturing ${durationSecs}s audio...", keyword)
    }
}
