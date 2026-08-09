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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import com.neubofy.veto.utils.AutoTheftDefenseManager

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

    // Main Protection States
    var isEnabled by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_ENABLED) as Boolean) }
    var simRemoved by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_SIM_REMOVED) as Boolean) }
    var lockMsg by remember { mutableStateOf((settings.get(Settings.SET_AUTO_THEFT_LOCK_MSG) as? String) ?: "") }
    var customTts by remember { mutableStateOf((settings.get(Settings.SET_AUTO_THEFT_CUSTOM_TTS) as? String) ?: "Theft suspected. Please unlock device to verify ownership.") }

    // Owner Contact States
    var contactPhone by remember { mutableStateOf((settings.get(Settings.SET_AUTO_THEFT_CONTACT_PHONE) as? String) ?: "") }
    var contactEmail by remember { mutableStateOf((settings.get(Settings.SET_AUTO_THEFT_CONTACT_EMAIL) as? String) ?: "") }
    var contactSocial by remember { mutableStateOf((settings.get(Settings.SET_AUTO_THEFT_CONTACT_SOCIAL) as? String) ?: "") }

    // Beta Features State
    var betaFailedUnlock by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_BETA_FAILED_UNLOCK) as Boolean) }
    var maxAttempts by remember { mutableStateOf((settings.get(Settings.SET_AUTO_THEFT_MAX_ATTEMPTS) as? Int) ?: 3) }

    fun saveAllSettings() {
        settings.set(Settings.SET_AUTO_THEFT_ENABLED, isEnabled)
        settings.set(Settings.SET_AUTO_THEFT_SIM_REMOVED, simRemoved)
        settings.set(Settings.SET_AUTO_THEFT_LOCK_MSG, lockMsg)
        settings.set(Settings.SET_AUTO_THEFT_CUSTOM_TTS, customTts)

        settings.set(Settings.SET_AUTO_THEFT_CONTACT_PHONE, contactPhone)
        settings.set(Settings.SET_AUTO_THEFT_CONTACT_EMAIL, contactEmail)
        settings.set(Settings.SET_AUTO_THEFT_CONTACT_SOCIAL, contactSocial)

        settings.set(Settings.SET_AUTO_THEFT_BETA_FAILED_UNLOCK, betaFailedUnlock)
        settings.set(Settings.SET_AUTO_THEFT_FAILED_UNLOCK, betaFailedUnlock)
        settings.set(Settings.SET_AUTO_THEFT_MAX_ATTEMPTS, maxAttempts)

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

            // Primary Trigger Criteria Card
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
                        Text("Primary Trigger Criteria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                }
            }

            // Custom Voice Announcement Card
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
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Custom Voice Announcement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = customTts,
                        onValueChange = { customTts = it },
                        label = { Text("Announcement Sentence") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Button(
                        onClick = {
                            if (customTts.isNotEmpty()) {
                                AutoTheftDefenseManager.speakTestTts(context, customTts)
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Announcement")
                    }
                }
            }

            // Owner Contact Information Card
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
                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Owner Contact Details (Displayed on Overlay)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Contact Phone Number") },
                        placeholder = { Text("+1234567890") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = contactEmail,
                        onValueChange = { contactEmail = it },
                        label = { Text("Contact Email Address") },
                        placeholder = { Text("owner@example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = contactSocial,
                        onValueChange = { contactSocial = it },
                        label = { Text("Social Media / Telegram ID") },
                        placeholder = { Text("@owner_id") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = lockMsg,
                        onValueChange = { lockMsg = it },
                        label = { Text("Custom Lock Screen Message") },
                        placeholder = { Text("This device is lost or stolen. Return to owner.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Beta Features Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Beta Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Failed Unlock Attempt Detection (Beta)", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("Trigger warning after incorrect PIN/Pattern attempts on stock Android", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                        }
                        Switch(checked = betaFailedUnlock, onCheckedChange = { betaFailedUnlock = it })
                    }

                    AnimatedVisibility(visible = betaFailedUnlock) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Max Failed Attempts Threshold",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
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
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )

                                FilledIconButton(
                                    onClick = { if (maxAttempts < 10) maxAttempts++ },
                                    enabled = maxAttempts < 10
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
