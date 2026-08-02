package com.neubofy.veto.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

object RingerUtils {
    @JvmStatic
    fun getRingtone(context: Context, ringtone: String): Ringtone {
        val r = RingtoneManager.getRingtone(context, Uri.parse(ringtone))
        val aa = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
            .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
            .build()
        r.audioAttributes = aa
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            r.isLooping = true
        }
        return r
    }

    @JvmStatic
    fun getDefaultRingtoneAsString(): String {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
    }
}
