package com.neubofy.veto.ui.settings

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neubofy.veto.R
import com.neubofy.veto.data.EncryptedSettingsRepository
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.VetoActivity
import com.neubofy.veto.ui.common.PasswordSetDialogCompose
import com.neubofy.veto.ui.theme.VetoTheme
import com.neubofy.veto.ui.theme.glassmorphism
import com.neubofy.veto.utils.CypherUtils
import java.util.Locale

class SettingsActivity : VetoActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var encSettings: EncryptedSettingsRepository

    private val ringtonePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                settings.set(Settings.SET_RINGER_TONE, uri.toString())
                updateRingtoneState(uri.toString())
            }
        }
    }

    private var _ringtoneName = mutableStateOf("Default")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = SettingsRepository.getInstance(this)
        encSettings = EncryptedSettingsRepository.getInstance(this)

        updateRingtoneState(settings.get(Settings.SET_RINGER_TONE) as String)

        setContent {
            VetoTheme {
                SettingsScreen(
                    settings = settings,
                    encSettings = encSettings,
                    ringtoneName = _ringtoneName.value,
                    onSelectRingtone = {
                        val currentUri = Uri.parse(settings.get(Settings.SET_RINGER_TONE) as String)
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                    onOpenLogs = { startActivity(Intent(this, LogViewActivity::class.java)) },
                    onOpenAbout = { startActivity(Intent(this, AboutActivity::class.java)) }
                )
            }
        }
    }

    private fun updateRingtoneState(uriStr: String) {
        val ringtone = RingtoneManager.getRingtone(this, Uri.parse(uriStr))
        _ringtoneName.value = ringtone?.getTitle(this) ?: "Default"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    encSettings: EncryptedSettingsRepository,
    ringtoneName: String,
    onSelectRingtone: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var wipeEnabled by remember { mutableStateOf(settings.get(Settings.SET_WIPE_ENABLED) as Boolean) }
    var pinEnabled by remember { mutableStateOf(settings.get(Settings.SET_ACCESS_VIA_PIN) as Boolean) }

    // Derived state from encrypted settings, force recomposition by tracking a random token or similar
    // For simplicity, we just use mutable state variables updated via side effects
    var wipePasswordSet by remember { mutableStateOf(!encSettings.getDeletePassword().isNullOrBlank()) }
    var pinSet by remember { mutableStateOf(!encSettings.getVetoPin().isNullOrBlank()) }

    var lockMessage by remember { mutableStateOf(settings.get(Settings.SET_LOCKSCREEN_MESSAGE) as String) }
    var triggerCommand by remember { mutableStateOf(settings.get(Settings.SET_Veto_COMMAND) as String) }

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showLockMessageDialog by remember { mutableStateOf(false) }
    var showCommandDialog by remember { mutableStateOf(false) }

    var showInfoDialogTitle by remember { mutableStateOf("") }
    var showInfoDialogMessage by remember { mutableStateOf("") }

    val context = LocalContext.current

    if (showInfoDialogTitle.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showInfoDialogTitle = "" },
            title = { Text(showInfoDialogTitle) },
            text = { Text(showInfoDialogMessage) },
            confirmButton = { TextButton(onClick = { showInfoDialogTitle = "" }) { Text("OK") } }
        )
    }

    if (showPasswordDialog) {
        PasswordSetDialogCompose(
            title = "Set Remote Delete Password",
            message = "Warning: Please remember this password. It is required to execute the remote wipe command.",
            positiveButtonText = "Set Password",
            minLength = CypherUtils.MIN_PASSWORD_LENGTH,
            onDismiss = { showPasswordDialog = false },
            onConfirm = { pw ->
                encSettings.setDeletePassword(pw)
                wipePasswordSet = !pw.isBlank()
                showPasswordDialog = false
            }
        )
    }

    if (showPinDialog) {
        PasswordSetDialogCompose(
            title = "Set Veto PIN",
            message = "Set a PIN to authorize commands sent via notification reply or unlisted contacts.",
            positiveButtonText = "Set PIN",
            minLength = 1,
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                encSettings.setVetoPin(pin)
                pinSet = !pin.isBlank()
                showPinDialog = false
            }
        )
    }

    if (showLockMessageDialog) {
        var tempMsg by remember { mutableStateOf(lockMessage) }
        AlertDialog(
            onDismissRequest = { showLockMessageDialog = false },
            title = { Text("Set Lock Screen Message") },
            text = {
                OutlinedTextField(
                    value = tempMsg,
                    onValueChange = { tempMsg = it },
                    label = { Text("Message") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    settings.set(Settings.SET_LOCKSCREEN_MESSAGE, tempMsg)
                    lockMessage = tempMsg
                    showLockMessageDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showLockMessageDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCommandDialog) {
        var tempCmd by remember { mutableStateOf(triggerCommand) }
        AlertDialog(
            onDismissRequest = { showCommandDialog = false },
            title = { Text("Set Trigger Command Word") },
            text = {
                OutlinedTextField(
                    value = tempCmd,
                    onValueChange = { tempCmd = it },
                    label = { Text("Command") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempCmd.isEmpty()) {
                        Toast.makeText(context, context.getString(R.string.Toast_Empty_LCLDCommand), Toast.LENGTH_LONG).show()
                        tempCmd = "veto"
                    }
                    val finalCmd = tempCmd.lowercase(Locale.ROOT)
                    settings.set(Settings.SET_Veto_COMMAND, finalCmd)
                    triggerCommand = finalCmd
                    showCommandDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCommandDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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

            // Remote Wipe Config
            item {
                SettingsCard(
                    title = "Remote Wipe Configuration",
                    icon = Icons.Default.Warning,
                    onInfoClick = {
                        showInfoDialogTitle = "Remote Wipe"
                        showInfoDialogMessage = context.getString(R.string.delete_pw_warning_no_backup) + "\n\n" + context.getString(R.string.Settings_LCLDCommand_Description)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Enable Remote Wipe", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = wipeEnabled,
                            onCheckedChange = {
                                wipeEnabled = it
                                settings.set(Settings.SET_WIPE_ENABLED, it)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Wipe Password", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (wipePasswordSet) "Password set" else "Not set",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            if (wipePasswordSet) {
                                IconButton(onClick = {
                                    encSettings.setDeletePassword(null)
                                    wipePasswordSet = false
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                            Button(onClick = { showPasswordDialog = true }) {
                                Text(if (wipePasswordSet) "Change" else "Set")
                            }
                        }
                    }
                }
            }

            // PIN Config
            item {
                SettingsCard(
                    title = "Veto PIN Configuration",
                    icon = Icons.Default.Lock,
                    onInfoClick = {
                        showInfoDialogTitle = "Veto PIN"
                        showInfoDialogMessage = context.getString(R.string.Settings_LCLD_via_Pin_Description)
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Require PIN for Commands", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = pinEnabled,
                            onCheckedChange = {
                                pinEnabled = it
                                settings.set(Settings.SET_ACCESS_VIA_PIN, it)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Veto PIN", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (pinSet) "PIN set" else "Not set",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            if (pinSet) {
                                IconButton(onClick = {
                                    encSettings.setVetoPin(null)
                                    pinSet = false
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                            Button(onClick = { showPinDialog = true }) {
                                Text(if (pinSet) "Change" else "Set")
                            }
                        }
                    }
                }
            }

            // Command Word & Lock Message
            item {
                SettingsCard(
                    title = "General Configurations",
                    icon = Icons.Default.Settings,
                    onInfoClick = null
                ) {
                    // Lock Message
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Lock Screen Message", style = MaterialTheme.typography.bodyLarge)
                                IconButton(onClick = {
                                    showInfoDialogTitle = "Lock Screen Message"
                                    showInfoDialogMessage = context.getString(R.string.Settings_Lockscreenmessage_Description)
                                }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                if (lockMessage.isNotEmpty()) "\"$lockMessage\"" else "Not set",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            if (lockMessage.isNotEmpty()) {
                                IconButton(onClick = {
                                    settings.set(Settings.SET_LOCKSCREEN_MESSAGE, "")
                                    lockMessage = ""
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                            Button(onClick = { showLockMessageDialog = true }) {
                                Text(if (lockMessage.isNotEmpty()) "Change" else "Set")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trigger Command
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Trigger Command Word", style = MaterialTheme.typography.bodyLarge)
                                IconButton(onClick = {
                                    showInfoDialogTitle = "Trigger Command"
                                    showInfoDialogMessage = context.getString(R.string.Settings_LCLDCommand_Description)
                                }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                                    Icon(Icons.Default.Info, contentDescription = "Info", modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                triggerCommand,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = { showCommandDialog = true }) {
                            Text("Edit")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Alarm Ringtone
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Siren Ringtone", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                ringtoneName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(onClick = onSelectRingtone) {
                            Text("Select")
                        }
                    }
                }
            }

            // App links
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphism(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("View Application Logs") },
                            leadingContent = { Icon(Icons.Default.Info, null) },
                            modifier = Modifier.clickable { onOpenLogs() },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ListItem(
                            headlineContent = { Text("About Veto") },
                            leadingContent = { Icon(Icons.Default.Phone, null) },
                            modifier = Modifier.clickable { onOpenAbout() },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    onInfoClick: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism(),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (onInfoClick != null) {
                    IconButton(onClick = onInfoClick) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            content()
        }
    }
}
