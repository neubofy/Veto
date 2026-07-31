package com.neubofy.veto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neubofy.veto.utils.Utils.Companion.copyToClipboard


const val EXTRA_TEXT_TO_COPY = "EXTRA_TEXT_TO_COPY"

class CopyInAppTextReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra(EXTRA_TEXT_TO_COPY) ?: return
        copyToClipboard(context, "", text)
    }
}
