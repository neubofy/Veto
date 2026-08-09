package com.neubofy.veto.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.VetoActivity

class AutoTheftSetupActivity : VetoActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = SettingsRepository.getInstance(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AutoTheftSetupScreen(
                        settings = settings,
                        onBackClicked = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTheftSetupScreen(
    settings: SettingsRepository,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current

    // Settings States
    var isEnabled by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_ENABLED) as Boolean) }
    var simRemoved by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_SIM_REMOVED) as Boolean) }
    var failedUnlock by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_FAILED_UNLOCK) as Boolean) }
    var maxAttempts by remember { mutableStateOf((settings.get(Settings.SET_AUTO_THEFT_MAX_ATTEMPTS) as? Int) ?: 3) }
    var lockMsg by remember { mutableStateOf((settings.get(Settings.SET_AUTO_THEFT_LOCK_MSG) as? String) ?: "") }

    fun saveAllSettings() {
        settings.set(Settings.SET_AUTO_THEFT_ENABLED, isEnabled)
        settings.set(Settings.SET_AUTO_THEFT_SIM_REMOVED, simRemoved)
        settings.set(Settings.SET_AUTO_THEFT_FAILED_UNLOCK, failedUnlock)
        settings.set(Settings.SET_AUTO_THEFT_MAX_ATTEMPTS, maxAttempts)
        settings.set(Settings.SET_AUTO_THEFT_LOCK_MSG, lockMsg)

        Toast.makeText(context, "Auto Theft settings saved successfully!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto Theft Protection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { saveAllSettings() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { saveAllSettings() },
                icon = { Icon(Icons.Default.Done, contentDescription = null) },
                text = { Text("Save Settings") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Device Admin Permission Warning Card
            val isDeviceAdminGranted = remember { com.neubofy.veto.permissions.DeviceAdminPermission().isGranted(context) }
            if (!isDeviceAdminGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Device Admin Permission Required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Text(
                            text = "Android OS requires Device Administrator permission to monitor failed PIN/Password unlock attempts and perform remote actions. Please activate Device Admin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = {
                                (context as? android.app.Activity)?.let {
                                    com.neubofy.veto.permissions.DeviceAdminPermission().request(it)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Activate Device Admin")
                        }
                    }
                }
            }

            // Master Switch Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto Theft Protection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isEnabled) "Protection Active" else "Protection Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }
            }

            // Detection Criteria Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger Criteria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider()

                    // SIM Removal Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("SIM Card Removal", fontWeight = FontWeight.Medium)
                            Text("Trigger warning overlay and sound alarm if SIM card is extracted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = simRemoved, onCheckedChange = { simRemoved = it })
                    }

                    HorizontalDivider()

                    // Failed Unlock Attempts Option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Max Failed Unlock Attempts", fontWeight = FontWeight.Medium)
                            Text("Trigger warning overlay after incorrect PIN/Pattern attempts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = failedUnlock, onCheckedChange = { failedUnlock = it })
                    }

                    AnimatedVisibility(visible = failedUnlock) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Max Failed Attempts Threshold",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FilledIconButton(
                                    onClick = { if (maxAttempts > 1) maxAttempts-- },
                                    enabled = maxAttempts > 1
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }

                                Text(
                                    text = "$maxAttempts attempts",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                FilledIconButton(
                                    onClick = { if (maxAttempts < 10) maxAttempts++ },
                                    enabled = maxAttempts < 10
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }

                            // Info tip for testing password attempts
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Notifications will alert you on every failed PIN/Password attempt with remaining attempts. The warning overlay can be dismissed ONLY by unlocking the device with the screen lock PIN/Password.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Lock Screen Message Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Message, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lock Screen Display Message", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = lockMsg,
                        onValueChange = { lockMsg = it },
                        label = { Text("Custom Lock Screen Message") },
                        placeholder = { Text("This device is lost or stolen. Return to owner.") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (lockMsg.isNotEmpty()) {
                                IconButton(onClick = { lockMsg = "" }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear Message")
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
