package com.neubofy.veto.ui.allowlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.neubofy.veto.R
import com.neubofy.veto.data.Contact

class AllowlistAdapter(
    private val onDeleteClicked: (String) -> Unit,
) : ListAdapter<AllowlistItem, AllowlistViewHolder>(AllowlistDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllowlistViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = inflater.inflate(R.layout.item_allowlist, parent, false)
        return AllowlistViewHolder(itemView, onDeleteClicked)
    }

    override fun onBindViewHolder(holder: AllowlistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun submitContactList(allowlist: List<Contact>) {
        val list = allowlist.map { contact -> AllowlistItem(contact.name, contact.number) }
        submitList(list)
    }

    object AllowlistDiffCallback : DiffUtil.ItemCallback<AllowlistItem>() {
        override fun areItemsTheSame(oldItem: AllowlistItem, newItem: AllowlistItem): Boolean {
            return oldItem.number == newItem.number
        }

        override fun areContentsTheSame(oldItem: AllowlistItem, newItem: AllowlistItem): Boolean {
            return oldItem == newItem
        }
    }
}