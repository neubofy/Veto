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
    var isSelected: Boolean,
    val category: String
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
        val etSearchApp = findViewById<android.widget.EditText>(R.id.etSearchApp)

        adapter = MessagingAppAdapter(filteredList) { updatedItem ->
            val currentAllowed = encRepo.getAllowedNotificationPackages().toMutableSet()
            if (updatedItem.isSelected) {
                currentAllowed.add(updatedItem.packageName)
            } else {
                currentAllowed.remove(updatedItem.packageName)
            }
            encRepo.setAllowedNotificationPackages(currentAllowed)
        }
        recyclerView.adapter = adapter

        etSearchApp.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                filterList(s.toString())
            }
        })

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
                if (!seenPackages.contains(pkgName) && isValidApp(pkgName)) {
                    seenPackages.add(pkgName)
                    val appName = resolveInfo.loadLabel(pm).toString()
                    val icon = resolveInfo.loadIcon(pm)
                    val isSelected = allowedPackages.contains(pkgName)
                    val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    val category = if (isMessagingPackage(pkgName)) "💬 Messaging" else if (isSystem) "⚙️ System" else "📱 User"
                    items.add(AppItem(appName, pkgName, icon, isSelected, category))
                }
            }

            // Also check installed applications for messaging packages
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            installedApps.forEach { appInfo ->
                val pkgName = appInfo.packageName
                if (!seenPackages.contains(pkgName) && isValidApp(pkgName)) {
                    seenPackages.add(pkgName)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    val isSelected = allowedPackages.contains(pkgName)
                    val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                    val category = if (isMessagingPackage(pkgName)) "💬 Messaging" else if (isSystem) "⚙️ System" else "📱 User"
                    items.add(AppItem(appName, pkgName, icon, isSelected, category))
                }
            }

            // Sort: Messaging apps first, then alphabetically
            items.sortBy { if (it.category.contains("Messaging")) "0_${it.appName.lowercase()}" else "1_${it.appName.lowercase()}" }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                appList.clear()
                appList.addAll(items)
                filterList(etSearchApp.text.toString())
            }
        }
    }

    private val filteredList = mutableListOf<AppItem>()

    private fun filterList(query: String) {
        filteredList.clear()
        if (query.isBlank()) {
            filteredList.addAll(appList)
        } else {
            val lowerQuery = query.lowercase()
            filteredList.addAll(appList.filter {
                it.appName.lowercase().contains(lowerQuery) || it.packageName.lowercase().contains(lowerQuery)
            })
        }
        adapter.notifyDataSetChanged()

        val tvEmptyState = findViewById<TextView>(R.id.tvEmptyState)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_apps)
        if (filteredList.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun isValidApp(pkg: String): Boolean {
        val lower = pkg.lowercase()

        // Exclude system overlays and base framework
        if (lower.contains("overlay") ||
            lower == "android" ||
            lower.startsWith("com.android.internal")
        ) {
            return false
        }
        return true
    }

    private fun isMessagingPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()

        // Known messaging application package identifiers
        val knownMessagingPackages = setOf(
            "com.whatsapp", "com.whatsapp.w4b",
            "org.telegram.messenger", "org.telegram.messenger.web", "org.telegram.plus",
            "org.thoughtcrime.securesms",
            "com.facebook.orca",
            "com.instagram.android",
            "com.discord",
            "com.viber.voip",
            "com.skype.raider",
            "com.tencent.mm",
            "jp.naver.line.android",
            "com.slack",
            "com.microsoft.teams",
            "com.snapchat.android",
            "chat.schildi.app",
            "im.vector.app",
            "org.session.session",
            "ch.threema.app"
        )

        if (knownMessagingPackages.contains(lower)) return true

        return lower.endsWith(".whatsapp") ||
                lower.endsWith(".telegram") ||
                lower.endsWith(".signal") ||
                lower.endsWith(".messenger")
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
            holder.imgIcon.setImageDrawable(item.icon)

            holder.tvPackage.text = item.packageName
            holder.tvPackage.visibility = View.VISIBLE
            
            holder.tvBadge.text = item.category
            holder.tvBadge.visibility = View.VISIBLE

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
