package com.neubofy.veto.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.neubofy.veto.R
import com.neubofy.veto.commands.Command


class CommandListAdapter(
    private val activity: AppCompatActivity,
) : ListAdapter<Command, CommandListViewHolder>(CommandListDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.item_command, parent, false)
        return CommandListViewHolder(activity, itemView);
    }

    override fun onBindViewHolder(holder: CommandListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object CommandListDiffCallback : DiffUtil.ItemCallback<Command>() {
        override fun areItemsTheSame(oldItem: Command, newItem: Command): Boolean {
            return oldItem.keyword == newItem.keyword
        }

        override fun areContentsTheSame(oldItem: Command, newItem: Command): Boolean {
            return oldItem.keyword == newItem.keyword
        }
    }
}
