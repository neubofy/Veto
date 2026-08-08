package com.neubofy.veto.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.neubofy.veto.ui.VetoActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.TextView
import androidx.compose.foundation.background
import com.neubofy.veto.BuildConfig
import com.neubofy.veto.R
import com.neubofy.veto.ui.theme.VetoTheme
import com.neubofy.veto.ui.theme.glassmorphism
import com.neubofy.veto.utils.UpdateManager
import com.neubofy.veto.utils.log
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class AboutActivity : VetoActivity() {
    companion object {
        const val ABOUT_MD_URL = "https://raw.githubusercontent.com/neubofy/Vito/main/ABOUT.md"
        const val GITHUB_PROFILE = "https://github.com/pawanwashudev-official"
        const val WEBSITE = "https://veto.neubofy.in"
        const val EMAIL = "support@neubofy.in"
        private val TAG = AboutActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VetoTheme {
                AboutScreen(
                    onBackClick = { finish() },
                    activity = this
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    activity: AppCompatActivity
) {
    val context = LocalContext.current
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatusText by remember { mutableStateOf("Keep Veto at its best") }

    var isCheckingBetaUpdate by remember { mutableStateOf(false) }
    var betaUpdateStatusText by remember { mutableStateOf("Get early access features") }

    var markdownContent by remember { mutableStateOf("") }
    val prefs = context.getSharedPreferences("about_cache", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        val cachedContent = prefs.getString("markdown_content", null)
        if (cachedContent != null) {
            markdownContent = cachedContent
        }

        try {
            val content = withContext(Dispatchers.IO) { URL(AboutActivity.ABOUT_MD_URL).readText() }
            markdownContent = content
            prefs.edit().putString("markdown_content", content).apply()
        } catch (e: Exception) {
            // Silently fail or log without using unresolvable extension method
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Veto", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().glassmorphism(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "V",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Veto Anti-Theft",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Updates
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().glassmorphism(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Software Updates",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        ListItem(
                            headlineContent = { Text("Check for Update") },
                            supportingContent = { Text(updateStatusText) },
                            trailingContent = {
                                if (isCheckingUpdate) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                else Icon(Icons.Default.Refresh, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                isCheckingUpdate = true
                                updateStatusText = "Checking for updates..."
                                UpdateManager.checkForUpdates(activity, silent = false, isBeta = false, onCheckComplete = {
                                    activity.runOnUiThread {
                                        isCheckingUpdate = false
                                        updateStatusText = "Keep Veto at its best"
                                    }
                                })
                            },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        ListItem(
                            headlineContent = { Text("Beta Updates") },
                            supportingContent = { Text(betaUpdateStatusText) },
                            trailingContent = {
                                if (isCheckingBetaUpdate) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                else Icon(Icons.Default.Refresh, contentDescription = null)
                            },
                            modifier = Modifier.clickable {
                                isCheckingBetaUpdate = true
                                betaUpdateStatusText = "Checking for beta updates..."
                                UpdateManager.checkForUpdates(activity, silent = false, isBeta = true, onCheckComplete = {
                                    activity.runOnUiThread {
                                        isCheckingBetaUpdate = false
                                        betaUpdateStatusText = "Get early access features"
                                    }
                                })
                            },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                    }
                }
            }

            // Markdown Content
            if (markdownContent.isNotEmpty()) {
                item {
                    AndroidView(
                        factory = { ctx ->
                            TextView(ctx).apply {
                                setTextColor(androidx.core.content.ContextCompat.getColor(ctx, android.R.color.darker_gray))
                                textSize = 14f
                            }
                        },
                        update = { tv ->
                            Markwon.create(context).setMarkdown(tv, markdownContent)
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Developer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().glassmorphism(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "MEET THE CREATOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "P",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Pawan Washudev",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Project Lead & Developer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Column {
                            ActionRow("GitHub Profile", Icons.Default.Person, { openUrl(context, AboutActivity.GITHUB_PROFILE) })
                            ActionRow("Website", Icons.Default.Info, { openUrl(context, AboutActivity.WEBSITE) })
                            ActionRow("Email Support", Icons.Default.Email, { sendEmail(context) })
                        }
                    }
                }
            }

            item {
                Text(
                    text = "© 2026 Neubofy. All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ActionRow(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}

fun sendEmail(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:${AboutActivity.EMAIL}")
        putExtra(Intent.EXTRA_SUBJECT, "Veto App - Support Request")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Email", AboutActivity.EMAIL)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}
