package com.neubofy.veto.ui.home

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.neubofy.veto.R
import com.neubofy.veto.commands.Command
import com.neubofy.veto.ui.setupPermissionsList


class CommandListViewHolder(
    private val activity: AppCompatActivity,
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {

    fun bind(item: Command) {
        val context = itemView.context

        itemView.findViewById<TextView>(R.id.usage).apply {
            text = item.usage
            val drawable = ContextCompat.getDrawable(context, item.icon)
            setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
        }

        itemView.findViewById<TextView>(R.id.description_short).text =
            context.getString(item.shortDescription)

        val textViewLongDescription = itemView.findViewById<TextView>(R.id.description_long)
        val longDesc = item.longDescription
        if (longDesc != null) {
            textViewLongDescription.text = context.getString(longDesc)
            textViewLongDescription.visibility = View.VISIBLE
        } else {
            textViewLongDescription.visibility = View.GONE
        }

        // Required permissions
        val permReqTitle = itemView.findViewById<TextView>(R.id.permissions_required_title)
        val permReqList = itemView.findViewById<LinearLayout>(R.id.permissions_required_list)
        setupPermissionsList(activity, permReqTitle, permReqList, item.requiredPermissions)

        // Optional permissions
        val permOptTitle = itemView.findViewById<TextView>(R.id.permissions_optional_title)
        val permOptList = itemView.findViewById<LinearLayout>(R.id.permissions_optional_list)
        setupPermissionsList(activity, permOptTitle, permOptList, item.optionalPermissions)
    }
}
