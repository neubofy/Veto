package com.neubofy.veto.ui.settings

import android.content.Intent
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
import com.neubofy.veto.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import com.neubofy.veto.ui.VetoActivity

data class AppItem(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    val isMessagingApp: Boolean,
    var isSelected: Boolean
)

class MessagingAppPickerActivity : VetoActivity() {

    private lateinit var encRepo: EncryptedSettingsRepository
    private val appList = mutableListOf<AppItem>()
    private lateinit var adapter: MessagingAppAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messaging_app_picker)

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar))

        encRepo = EncryptedSettingsRepository.getInstance(this)
        val allowedPackages = encRepo.getAllowedNotificationPackages()

        val pm = packageManager
        val seenPackages = mutableSetOf<String>()

        // Method 1: Query Launcher Intent Activities
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launcherApps = pm.queryIntentActivities(mainIntent, 0)
        launcherApps.forEach { resolveInfo ->
            val pkgName = resolveInfo.activityInfo.packageName
            if (!seenPackages.contains(pkgName)) {
                seenPackages.add(pkgName)
                val appName = resolveInfo.loadLabel(pm).toString()
                val icon = resolveInfo.loadIcon(pm)
                val isMsg = isMessagingPackage(pkgName)
                val isSelected = allowedPackages.contains(pkgName)
                appList.add(AppItem(appName, pkgName, icon, isMsg, isSelected))
            }
        }

        // Method 2: Query Installed Applications fallback for non-launcher apps
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        installedApps.forEach { appInfo ->
            val pkgName = appInfo.packageName
            if (!seenPackages.contains(pkgName) && !isSystemInternalPackage(pkgName)) {
                seenPackages.add(pkgName)
                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val isMsg = isMessagingPackage(pkgName)
                val isSelected = allowedPackages.contains(pkgName)
                appList.add(AppItem(appName, pkgName, icon, isMsg, isSelected))
            }
        }

        // Prioritize Messaging apps at the top, then sort alphabetically
        appList.sortWith(Comparator { a, b ->
            if (a.isMessagingApp && !b.isMessagingApp) {
                -1
            } else if (!a.isMessagingApp && b.isMessagingApp) {
                1
            } else {
                a.appName.compareTo(b.appName, ignoreCase = true)
            }
        })

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

    private fun isSystemInternalPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower == "android" || lower.startsWith("com.android.systemui") || lower.startsWith("com.android.providers")
    }

    private fun isMessagingPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("whatsapp") || pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b" ||
                lower.contains("telegram") || lower.contains("signal") ||
                lower.contains("messenger") || lower.contains("discord") || lower.contains("viber") ||
                lower.contains("instagram") || lower.contains("skype") || lower.contains("slack") ||
                lower.contains("teams") || lower.contains("line") || lower.contains("wechat") ||
                lower.contains("snapchat") || lower.contains("message") || lower.contains("sms") ||
                lower.contains("chat")
    }

    inner class MessagingAppAdapter(
        private val items: List<AppItem>,
        private val onSelectionChanged: (AppItem) -> Unit
    ) : RecyclerView.Adapter<MessagingAppAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgIcon: ImageView = view.findViewById(R.id.imgAppIcon)
            val tvName: TextView = view.findViewById(R.id.tvAppName)
            val tvPackage: TextView = view.findViewById(R.id.tvPackageName)
            val tvBadge: TextView = view.findViewById(R.id.tvBadge)
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
            
            if (item.isMessagingApp) {
                holder.tvBadge.visibility = View.VISIBLE
            } else {
                holder.tvBadge.visibility = View.GONE
            }

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
