package com.neubofy.veto.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.neubofy.veto.R
import com.neubofy.veto.transports.Transport


class TransportListAdapter(
    private val activity: AppCompatActivity,
) : ListAdapter<Transport<*>, TransportListViewHolder>(CommandListDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransportListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.item_transport, parent, false)
        return TransportListViewHolder(activity, itemView);
    }

    override fun onBindViewHolder(holder: TransportListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object CommandListDiffCallback : DiffUtil.ItemCallback<Transport<*>>() {
        override fun areItemsTheSame(oldItem: Transport<*>, newItem: Transport<*>): Boolean {
            return oldItem.getDestinationString() == newItem.getDestinationString()
        }

        override fun areContentsTheSame(oldItem: Transport<*>, newItem: Transport<*>): Boolean {
            return oldItem.getDestinationString() == newItem.getDestinationString()
        }
    }
}
