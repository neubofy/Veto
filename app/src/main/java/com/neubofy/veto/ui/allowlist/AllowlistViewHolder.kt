package com.neubofy.veto.ui.allowlist

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.neubofy.veto.R
import com.neubofy.veto.utils.Utils.Companion.copyToClipboard

class AllowlistViewHolder(
    itemView: View,
    private val onDeleteClicked: (String) -> Unit,
) : RecyclerView.ViewHolder(itemView) {

    fun bind(item: AllowlistItem) {
        itemView.findViewById<TextView>(R.id.text_name).text = item.name

        itemView.findViewById<TextView>(R.id.text_number).apply {
            text = item.number
            setOnLongClickListener { v ->
                copyToClipboard(
                    v.context,
                    v.context.getString(R.string.allowlist_phone_number),
                    item.number
                )
                return@setOnLongClickListener true
            }
        }

        itemView.findViewById<ImageView>(R.id.button_delete)
            .setOnClickListener { onDeleteClicked(item.number) }
    }
}
