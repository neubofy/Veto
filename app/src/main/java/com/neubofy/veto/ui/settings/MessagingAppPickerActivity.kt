package com.neubofy.veto.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.neubofy.veto.ui.VetoActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.neubofy.veto.data.EncryptedSettingsRepository
import com.neubofy.veto.ui.theme.VetoTheme
import com.neubofy.veto.ui.theme.glassmorphism
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        encRepo = EncryptedSettingsRepository.getInstance(this)

        setContent {
            VetoTheme {
                MessagingAppPickerScreen(
                    encRepo = encRepo,
                    pm = packageManager,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagingAppPickerScreen(
    encRepo: EncryptedSettingsRepository,
    pm: PackageManager,
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val allowedPackages = encRepo.getAllowedNotificationPackages()
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
                apps = items
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Allowed Messaging Apps", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Select which messaging apps Veto is allowed to listen to and reply from.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (apps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No supported messaging apps (WhatsApp, Telegram, Signal, Messenger, etc.) found on device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps, key = { it.packageName }) { appItem ->
                        var isSelected by remember { mutableStateOf(appItem.isSelected) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glassmorphism()
                                .clickable {
                                    isSelected = !isSelected
                                    appItem.isSelected = isSelected

                                    val currentAllowed = encRepo.getAllowedNotificationPackages().toMutableSet()
                                    if (isSelected) {
                                        currentAllowed.add(appItem.packageName)
                                    } else {
                                        currentAllowed.remove(appItem.packageName)
                                    }
                                    encRepo.setAllowedNotificationPackages(currentAllowed)
                                },
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = appItem.icon.toBitmap(config = android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap(),
                                    contentDescription = "App Icon",
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = appItem.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        isSelected = checked
                                        appItem.isSelected = checked

                                        val currentAllowed = encRepo.getAllowedNotificationPackages().toMutableSet()
                                        if (checked) {
                                            currentAllowed.add(appItem.packageName)
                                        } else {
                                            currentAllowed.remove(appItem.packageName)
                                        }
                                        encRepo.setAllowedNotificationPackages(currentAllowed)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isMessagingPackage(pkg: String): Boolean {
    val lower = pkg.lowercase()

    // Exclude system overlays, framework, telephony providers, and vendor packages
    if (lower.contains("overlay") ||
        lower.startsWith("android") ||
        lower.startsWith("com.android.internal") ||
        lower.startsWith("com.android.providers") ||
        lower.contains("vendor") ||
        lower.contains("systemui")
    ) {
        return false
    }

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
