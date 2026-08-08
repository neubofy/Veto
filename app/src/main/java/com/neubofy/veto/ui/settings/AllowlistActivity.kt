package com.neubofy.veto.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.neubofy.veto.ui.VetoActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.neubofy.veto.R
import com.neubofy.veto.data.AllowlistRepository
import com.neubofy.veto.data.Contact
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.theme.VetoTheme
import com.neubofy.veto.ui.theme.glassmorphism

class AllowlistActivity : VetoActivity() {

    private lateinit var allowlistRepository: AllowlistRepository
    private lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        allowlistRepository = AllowlistRepository.getInstance(this)
        settings = SettingsRepository.getInstance(this)

        setContent {
            VetoTheme {
                AllowlistScreen(
                    repository = allowlistRepository,
                    settings = settings,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowlistScreen(
    repository: AllowlistRepository,
    settings: SettingsRepository,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(repository.list.toList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTipDialog by remember { mutableStateOf(false) }

    if (showTipDialog) {
        val keyword = settings.get(Settings.SET_Veto_COMMAND) as String
        AlertDialog(
            onDismissRequest = {
                showTipDialog = false
                settings.set(Settings.SET_FIRST_TIME_CONTACT_ADDED, true)
            },
            title = { Text("Tip") },
            text = { Text(context.getString(R.string.tip_first_contact_added, keyword, keyword, keyword)) },
            confirmButton = {
                TextButton(onClick = {
                    showTipDialog = false
                    settings.set(Settings.SET_FIRST_TIME_CONTACT_ADDED, true)
                }) {
                    Text("OK")
                }
            }
        )
    }

    if (showAddDialog) {
        var nameInput by remember { mutableStateOf("") }
        var phoneInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(context.getString(R.string.allowlist_add_phone_number)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val dummyContact = Contact.from(context, nameInput, phoneInput)
                    if (dummyContact == null) {
                        Toast.makeText(context, R.string.allowlist_invalid_number, Toast.LENGTH_LONG).show()
                    } else {
                        if (!repository.contains(dummyContact)) {
                            repository.add(dummyContact)
                            contacts = repository.list.toList()
                            if (settings.get(Settings.SET_FIRST_TIME_CONTACT_ADDED) as Boolean == false) {
                                showTipDialog = true
                            }
                        } else {
                            Toast.makeText(context, R.string.Toast_Duplicate_contact, Toast.LENGTH_LONG).show()
                        }
                    }
                    showAddDialog = false
                }) {
                    Text(context.getString(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(context.getString(R.string.Settings_WhiteList), fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Contact")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .glassmorphism(),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = context.getString(R.string.Whitelist_help),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = context.getString(R.string.Whitelist_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contacts, key = { it.number }) { contact ->
                        ContactItem(
                            contact = contact,
                            onDelete = {
                                repository.remove(contact.number)
                                contacts = repository.list.toList()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = contact.number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
