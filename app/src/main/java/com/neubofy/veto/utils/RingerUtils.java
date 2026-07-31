package com.neubofy.veto.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class RingerUtils {

    public static Ringtone getRingtone(Context context, String ringtone) {
        Ringtone r = RingtoneManager.getRingtone(context, Uri.parse(ringtone));
        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .build();
        r.setAudioAttributes(aa);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            r.setLooping(true);
        }
        return r;
    }

    public static String getDefaultRingtoneAsString() {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
    }

}
