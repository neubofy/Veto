package com.neubofy.veto.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.neubofy.veto.R
import com.neubofy.veto.data.LogEntry


class LogViewAdapter : ListAdapter<LogEntry, LogViewViewHolder>(LogViewDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_log_entry, parent, false);
        return LogViewViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object LogViewDiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry): Boolean {
            return oldItem == newItem
        }
    }
}
