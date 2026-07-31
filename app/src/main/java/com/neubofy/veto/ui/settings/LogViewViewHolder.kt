package com.neubofy.veto.ui.settings

import android.annotation.SuppressLint
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.neubofy.veto.R
import com.neubofy.veto.data.LogEntry
import com.neubofy.veto.utils.Utils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class LogViewViewHolder(
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {

    @SuppressLint("SetTextI18n")
    fun bind(item: LogEntry) {
        itemView.findViewById<TextView>(R.id.log_level_and_tag).apply {
            // For now, don't show the level. It just takes space and doesn't add much.
            text = item.tag
        }

        itemView.findViewById<TextView>(R.id.log_time).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val instant = Instant.ofEpochMilli(item.timeMillis)
                val zoned = instant.atZone(ZoneId.systemDefault())

                val date = zoned.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val time = zoned.format(DateTimeFormatter.ISO_LOCAL_TIME)
                text = "$date $time"
            } else {
                // No pretty display for Android < 8
                text = item.timeMillis.toString()
            }
        }

        itemView.findViewById<TextView>(R.id.log_msg).apply {
            text = item.msg
        }

        itemView.setOnLongClickListener {
            Utils.copyToClipboard(it.context, "", item.msg)
            return@setOnLongClickListener true
        }
    }
}