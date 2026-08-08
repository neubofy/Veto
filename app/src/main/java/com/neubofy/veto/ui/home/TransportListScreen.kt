package com.neubofy.veto.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import com.neubofy.veto.data.EncryptedSettingsRepository
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.transports.SmsTransport
import com.neubofy.veto.transports.NotificationReplyTransport
import com.neubofy.veto.transports.NextJsServerTransport
import com.neubofy.veto.ui.theme.glassmorphism

@Composable
fun TransportListScreen(
    transports: List<Transport<*>>,
    activity: AppCompatActivity
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(transports, key = { it.getDestinationString() }) { transport ->
            TransportItem(transport = transport, activity = activity)
        }
    }
}

@Composable
fun TransportItem(
    transport: Transport<*>,
    activity: AppCompatActivity
) {
    val context = LocalContext.current
    var showInfo by remember { mutableStateOf(false) }

    val encRepo = remember { EncryptedSettingsRepository.getInstance(context) }
    val transportKey = when (transport) {
        is SmsTransport -> "sms"
        is NotificationReplyTransport -> "notification_reply"
        is NextJsServerTransport -> "cloud"
        else -> "inapp"
    }

    var isEnabled by remember { mutableStateOf(encRepo.isTransportEnabled(transportKey)) }

    if (showInfo) {
        val fullDescription = buildString {
            append(transport.description)
            val note = transport.descriptionNote
            if (note != null) append("\n\nNote: $note")
            val auth = transport.descriptionAuth
            if (auth != null) append("\n\nAuth: $auth")
        }

        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(context.getString(transport.title)) },
            text = { Text(fullDescription) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://veto.neubofy.in/#transports"))
                    activity.startActivity(intent)
                    showInfo = false
                }) {
                    Text("Read on Website")
                }
            }
        )
    }

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
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = transport.icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = context.getString(transport.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showInfo = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        isEnabled = checked
                        encRepo.setTransportEnabled(transportKey, checked)
                        if (checked) {
                            val missing = transport.missingRequiredPermissions(context)
                            if (missing.isNotEmpty()) {
                                missing.firstOrNull()?.request(activity)
                            }
                        }
                    }
                )
            }

            if (transport.requiredPermissions.isNotEmpty()) {
                Text(
                    text = "Required Permissions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                transport.requiredPermissions.forEach { permission ->
                    PermissionItem(permission = permission, activity = activity)
                }
            }

            if (transport.actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    transport.actions.forEach { action ->
                        Button(
                            onClick = { action.run(activity) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(context.getString(action.titleResourceId))
                        }
                    }
                }
            }
        }
    }
}
