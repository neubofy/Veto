package com.neubofy.veto.ui.home

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.neubofy.veto.R
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.ui.setupPermissionsList
import com.neubofy.veto.utils.WebUtils


class TransportListViewHolder(
    private val activity: AppCompatActivity,
    itemView: View,
) : RecyclerView.ViewHolder(itemView) {

    fun bind(item: Transport<*>) {
        val context = itemView.context

        itemView.findViewById<TextView>(R.id.title).apply {
            text = context.getString(item.title)
            val drawable = ContextCompat.getDrawable(context, item.icon)
            setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
        }

        itemView.findViewById<View>(R.id.transport_info_icon).setOnClickListener {
            val fullDescription = buildString {
                append(item.description)
                val note = item.descriptionNote
                if (note != null) append("\n\nNote: $note")
                val auth = item.descriptionAuth
                if (auth != null) append("\n\nAuth: $auth")
            }

            com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(item.title))
                .setMessage(fullDescription)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton("Read on Website") { _, _ ->
                    WebUtils.openCustomTab(context, "https://veto.neubofy.in/#transports")
                }
                .show()
        }

        val permReqTitle = itemView.findViewById<TextView>(R.id.permissions_required_title)
        val permReqList = itemView.findViewById<LinearLayout>(R.id.permissions_required_list)
        setupPermissionsList(activity, permReqTitle, permReqList, item.requiredPermissions, true)

        // Bind Master Switch
        val encRepo = com.neubofy.veto.data.EncryptedSettingsRepository.getInstance(context)
        val transportKey = when (item) {
            is com.neubofy.veto.transports.SmsTransport -> "sms"
            is com.neubofy.veto.transports.NotificationReplyTransport -> "notification_reply"
            is com.neubofy.veto.transports.NextJsServerTransport -> "cloud"
            else -> "inapp"
        }

        val switchMaster = itemView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_transport_master)
        switchMaster.setOnCheckedChangeListener(null)
        switchMaster.isChecked = encRepo.isTransportEnabled(transportKey)

        switchMaster.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                if (transportKey == "sms") {
                    val allowlistRepo = com.neubofy.veto.data.AllowlistRepository.getInstance(context)
                    if (allowlistRepo.list.none { it.isStarred }) {
                        compoundButton.isChecked = false
                        android.widget.Toast.makeText(context, "Must have a starred contact in Allowlist to enable SMS", android.widget.Toast.LENGTH_LONG).show()
                        return@setOnCheckedChangeListener
                    }
                } else if (transportKey == "notification_reply") {
                    if (encRepo.getAllowedNotificationPackages().isEmpty()) {
                        compoundButton.isChecked = false
                        android.widget.Toast.makeText(context, "Must add a messaging app in settings first", android.widget.Toast.LENGTH_LONG).show()
                        return@setOnCheckedChangeListener
                    }
                } else if (transportKey == "cloud") {
                    if (!com.neubofy.veto.transports.NextJsServerTransport.isConnected(context)) {
                        compoundButton.isChecked = false
                        android.widget.Toast.makeText(context, "Must connect to server in Account Settings first", android.widget.Toast.LENGTH_LONG).show()
                        return@setOnCheckedChangeListener
                    }
                }
            }

            encRepo.setTransportEnabled(transportKey, isChecked)
            if (isChecked) {
                val missing = item.missingRequiredPermissions(context)
                if (missing.isNotEmpty()) {
                    missing.firstOrNull()?.request(activity)
                }
            }
        }

        setupActions(item)
    }

    private fun setupActions(item: Transport<*>) {
        val context = itemView.context

        val actions = item.actions
        val actionsLayout = itemView.findViewById<LinearLayout>(R.id.actions_list)

        if (actions.isEmpty()) {
            actionsLayout.visibility = View.GONE
        } else {
            actionsLayout.visibility = View.VISIBLE
            actionsLayout.removeAllViews()

            val inflater = LayoutInflater.from(context)
            for (a in actions) {
                val view = inflater.inflate(R.layout.item_transport_action, actionsLayout, true)
                view.findViewById<Button>(R.id.action_button).apply {
                    text = context.getString(a.titleResourceId)
                    setOnClickListener { _ -> a.run(activity) }
                }
            }
        }
    }
}
