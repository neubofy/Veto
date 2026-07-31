package com.neubofy.veto.commands

import android.content.Context
import android.media.AudioManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.permissions.DoNotDisturbAccessPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.log


/**
 * ## Note
 *
 * RINGER_MODE_SILENT also enables DND mode.
 * This is Android's opinionated default.
 *
 * We try to work around this in "veto ring", because there our goal is to restore the user's settings.
 * Here, we stay with Android's default (which is enabling DND).
 *
 * References:
 *
 * - https://gitlab.com/veto-foss/veto-android/-/merge_requests/342
 * - https://stackoverflow.com/questions/58044974/enable-silent-mode-in-android-without-triggering-do-not-disturb
 * - https://issuetracker.google.com/issues/237819541
 */
class RingerModeCommand(context: Context) : Command(context) {
    companion object {
        private val TAG = RingerModeCommand::class.simpleName
    }

    override val keyword = "ringermode"
    override val usage = "ringermode [normal | vibrate | silent]"

    @get:DrawableRes
    override val icon = R.drawable.ic_vibration

    @get:StringRes
    override val shortDescription = R.string.cmd_ringermode_description_short

    override val longDescription = R.string.cmd_ringermode_description_long

    override val requiredPermissions = listOf(DoNotDisturbAccessPermission())

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val audioManager = context.getSystemService(AudioManager::class.java)

        val oldMode = audioManager.ringerMode

        if (args.isEmpty()) {
            val msg = context.getString(
                R.string.cmd_ringermode_response_empty,
                ringerModeToString(oldMode),
            )
            context.log().i(TAG, msg)
            transport.send(context, msg, keyword)
            return
        }

        val newMode = when {
            args.contains("normal") -> AudioManager.RINGER_MODE_NORMAL
            args.contains("vibrate") -> AudioManager.RINGER_MODE_VIBRATE
            args.contains("silent") -> AudioManager.RINGER_MODE_SILENT
            else -> {
                // Do nothing. The response message will indirectly indicate that it is unchanged.
                oldMode
            }
        }

        audioManager.ringerMode = newMode

        val msg = context.getString(
            R.string.cmd_ringermode_response,
            ringerModeToString(oldMode),
            ringerModeToString(newMode)
        )
        context.log().i(TAG, msg)
        transport.send(context, msg, keyword)
    }
}

fun ringerModeToString(mode: Int): String {
    // These strings are deliberately NOT translated, in order to match the command
    return when (mode) {
        AudioManager.RINGER_MODE_NORMAL -> "normal"
        AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
        AudioManager.RINGER_MODE_SILENT -> "silent"
        else -> "??"
    }
}
