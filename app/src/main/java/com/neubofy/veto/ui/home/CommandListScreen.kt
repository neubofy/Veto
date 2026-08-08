package com.neubofy.veto.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.appcompat.app.AppCompatActivity
import com.neubofy.veto.commands.Command
import com.neubofy.veto.permissions.Permission
import com.neubofy.veto.ui.theme.glassmorphism

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandListScreen(
    commands: List<Command>,
    activity: AppCompatActivity
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Commands", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(commands, key = { it.keyword }) { command ->
                CommandItem(command = command, activity = activity)
            }
        }
    }
}

@Composable
fun CommandItem(
    command: Command,
    activity: AppCompatActivity
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(context.getString(command.shortDescription)) },
            text = { Text(if (command.longDescription != null) context.getString(command.longDescription!!) else context.getString(command.shortDescription)) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://veto.neubofy.in/#cmd-${command.keyword}"))
                    activity.startActivity(intent)
                    showDialog = false
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
                    painter = painterResource(id = command.icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = command.usage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Help",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = context.getString(command.shortDescription),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            if (command.requiredPermissions.isNotEmpty()) {
                Text(
                    text = "Required Permissions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                command.requiredPermissions.forEach { permission ->
                    PermissionItem(permission = permission, activity = activity)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (command.optionalPermissions.isNotEmpty()) {
                Text(
                    text = "Optional Permissions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                command.optionalPermissions.forEach { permission ->
                    PermissionItem(permission = permission, activity = activity)
                }
            }
        }
    }
}

@Composable
fun PermissionItem(
    permission: Permission,
    activity: AppCompatActivity
) {
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(permission.isGranted(context)) }
    var showInfo by remember { mutableStateOf(false) }

    // Re-check permission status when returning to the app
    DisposableEffect(Unit) {
        // We could add a lifecycle observer here if needed to update isGranted automatically
        // For now, it evaluates initially.
        onDispose { }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(context.getString(permission.name)) },
            text = { Text(if (permission.description != null) context.getString(permission.description!!) else context.getString(permission.name)) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("OK")
                }
            }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = context.getString(permission.name),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = { showInfo = true },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Permission Info",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Button(
                onClick = {
                    permission.request(activity)
                    // Note: State won't update immediately here because the OS dialog is shown
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Grant", fontSize = 12.sp)
            }
        }
    }
}
