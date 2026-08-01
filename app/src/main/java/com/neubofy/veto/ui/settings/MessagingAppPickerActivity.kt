package com.neubofy.veto.ui.settings

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.neubofy.veto.R
import com.neubofy.veto.data.EncryptedSettingsRepository
import com.neubofy.veto.ui.VetoActivity

data class AppItem(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    var isSelected: Boolean
)

class MessagingAppPickerActivity : VetoActivity() {

    private lateinit var encRepo: EncryptedSettingsRepository
    private val appList = mutableListOf<AppItem>()
    private lateinit var adapter: MessagingAppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messaging_app_picker)

        encRepo = EncryptedSettingsRepository.getInstance(this)
        val allowedPackages = encRepo.getAllowedNotificationPackages()

        val pm = packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        installedApps.forEach { appInfo ->
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            
            // Show non-system apps or common messaging apps
            if (!isSystem || isUpdatedSystem || isMessagingPackage(appInfo.packageName)) {
                val name = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val isSelected = allowedPackages.contains(appInfo.packageName)
                appList.add(AppItem(name, appInfo.packageName, icon, isSelected))
            }
        }

        appList.sortBy { it.appName }

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_apps)
        adapter = MessagingAppAdapter(appList) { updatedItem ->
            val currentAllowed = encRepo.getAllowedNotificationPackages().toMutableSet()
            if (updatedItem.isSelected) {
                currentAllowed.add(updatedItem.packageName)
            } else {
                currentAllowed.remove(updatedItem.packageName)
            }
            encRepo.setAllowedNotificationPackages(currentAllowed)
        }
        recyclerView.adapter = adapter
    }

    private fun isMessagingPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("whatsapp") || lower.contains("telegram") || lower.contains("signal") ||
                lower.contains("message") || lower.contains("sms") || lower.contains("chat") ||
                lower.contains("messenger") || lower.contains("discord") || lower.contains("viber")
    }

    inner class MessagingAppAdapter(
        private val items: List<AppItem>,
        private val onSelectionChanged: (AppItem) -> Unit
    ) : RecyclerView.Adapter<MessagingAppAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgIcon: ImageView = view.findViewById(R.id.imgAppIcon)
            val tvName: TextView = view.findViewById(R.id.tvAppName)
            val tvPackage: TextView = view.findViewById(R.id.tvPackageName)
            val cbSelected: MaterialCheckBox = view.findViewById(R.id.cbAppSelected)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_messaging_app, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.appName
            holder.tvPackage.text = item.packageName
            holder.imgIcon.setImageDrawable(item.icon)
            holder.cbSelected.setOnCheckedChangeListener(null)
            holder.cbSelected.isChecked = item.isSelected

            holder.cbSelected.setOnCheckedChangeListener { _, isChecked ->
                item.isSelected = isChecked
                onSelectionChanged(item)
            }

            holder.itemView.setOnClickListener {
                holder.cbSelected.isChecked = !holder.cbSelected.isChecked
            }
        }

        override fun getItemCount() = items.size
    }
}
