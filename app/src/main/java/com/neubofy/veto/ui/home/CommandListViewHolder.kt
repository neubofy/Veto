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

        // Command enable / disable toggle switch
        val switchToggle = itemView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.command_toggle_switch)
        val settings = com.neubofy.veto.data.SettingsRepository.getInstance(context)

        val settingKey = when (item.keyword.lowercase()) {
            "delete" -> com.neubofy.veto.data.Settings.SET_CMD_DELETE_ENABLED
            "lock" -> com.neubofy.veto.data.Settings.SET_CMD_LOCK_ENABLED
            "audio" -> com.neubofy.veto.data.Settings.SET_CMD_AUDIO_ENABLED
            "photo", "camera" -> com.neubofy.veto.data.Settings.SET_CMD_PHOTO_ENABLED
            "video" -> com.neubofy.veto.data.Settings.SET_CMD_VIDEO_ENABLED
            "gps" -> com.neubofy.veto.data.Settings.SET_CMD_GPS_ENABLED
            "bluetooth" -> com.neubofy.veto.data.Settings.SET_CMD_BLUETOOTH_ENABLED
            "ring" -> com.neubofy.veto.data.Settings.SET_CMD_RING_ENABLED
            "flash" -> com.neubofy.veto.data.Settings.SET_CMD_FLASH_ENABLED
            "locate" -> com.neubofy.veto.data.Settings.SET_CMD_LOCATE_ENABLED
            "stats" -> com.neubofy.veto.data.Settings.SET_CMD_STATS_ENABLED
            "theft" -> com.neubofy.veto.data.Settings.SET_CMD_THEFT_ENABLED
            "autoloc" -> com.neubofy.veto.data.Settings.SET_CMD_AUTOLOC_ENABLED
            "ringer", "ringermode" -> com.neubofy.veto.data.Settings.SET_CMD_RINGERMODE_ENABLED
            else -> com.neubofy.veto.data.Settings.SET_CMD_RING_ENABLED
        }

        switchToggle.visibility = View.VISIBLE
        switchToggle.setOnCheckedChangeListener(null)
        val defaultVal = when (item.keyword.lowercase()) {
            "delete", "lock", "audio", "photo", "camera", "video", "gps", "bluetooth" -> false
            else -> true
        }
        switchToggle.isChecked = settings.get(settingKey) as? Boolean ?: defaultVal
        switchToggle.setOnCheckedChangeListener { _, isChecked ->
            settings.set(settingKey, isChecked)
            val stateText = if (isChecked) "Enabled" else "Disabled"
            android.widget.Toast.makeText(context, "$stateText ${item.keyword}", android.widget.Toast.LENGTH_SHORT).show()
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
