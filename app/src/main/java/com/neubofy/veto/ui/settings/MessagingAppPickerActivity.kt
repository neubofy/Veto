package com.neubofy.veto.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.neubofy.veto.R
import com.neubofy.veto.data.EncryptedSettingsRepository
import com.neubofy.veto.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import com.neubofy.veto.ui.VetoActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar))

        encRepo = EncryptedSettingsRepository.getInstance(this)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvEmptyState = findViewById<TextView>(R.id.tvEmptyState)
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

        // Load apps asynchronously on IO thread to prevent UI freezing
        lifecycleScope.launch(Dispatchers.IO) {
            val allowedPackages = encRepo.getAllowedNotificationPackages()
            val pm = packageManager
            val seenPackages = mutableSetOf<String>()
            val items = mutableListOf<AppItem>()

            // Query launcher intent activities
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherApps = pm.queryIntentActivities(mainIntent, 0)

            launcherApps.forEach { resolveInfo ->
                val pkgName = resolveInfo.activityInfo.packageName
                if (!seenPackages.contains(pkgName) && isMessagingPackage(pkgName)) {
                    seenPackages.add(pkgName)
                    val appName = resolveInfo.loadLabel(pm).toString()
                    val icon = resolveInfo.loadIcon(pm)
                    val isSelected = allowedPackages.contains(pkgName)
                    items.add(AppItem(appName, pkgName, icon, isSelected))
                }
            }

            // Also check installed applications for messaging packages
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            installedApps.forEach { appInfo ->
                val pkgName = appInfo.packageName
                if (!seenPackages.contains(pkgName) && isMessagingPackage(pkgName)) {
                    seenPackages.add(pkgName)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    val isSelected = allowedPackages.contains(pkgName)
                    items.add(AppItem(appName, pkgName, icon, isSelected))
                }
            }

            items.sortBy { it.appName.lowercase() }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                appList.clear()
                appList.addAll(items)
                adapter.notifyDataSetChanged()

                if (items.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun isMessagingPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b" || lower.contains("whatsapp") ||
                pkg == "org.telegram.messenger" || lower.contains("telegram") ||
                pkg == "org.thoughtcrime.securesms" || lower.contains("signal") ||
                pkg == "com.facebook.orca" || lower.contains("messenger") ||
                pkg == "com.instagram.android" || lower.contains("instagram") ||
                pkg == "com.discord" || lower.contains("discord") ||
                pkg == "com.viber.voip" || lower.contains("viber") ||
                pkg == "com.skype.raider" || lower.contains("skype") ||
                pkg == "com.tencent.mm" || lower.contains("wechat") ||
                pkg == "jp.naver.line.android" || lower.contains("line") ||
                pkg == "com.slack" || lower.contains("slack") ||
                pkg == "com.microsoft.teams" || lower.contains("teams") ||
                pkg == "com.snapchat.android" || lower.contains("snapchat")
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
            holder.tvBadge.visibility = View.VISIBLE
            holder.tvBadge.text = "💬 Messaging App"

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
