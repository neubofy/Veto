package com.neubofy.veto.ui.home

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.neubofy.veto.ui.theme.glassmorphism
import com.neubofy.veto.utils.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPageScreen(
    onOpenCommands: () -> Unit,
    onOpenTransports: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Veto", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DeviceStatusCard()

            ActionCard(
                title = "Commands",
                icon = Icons.Default.List,
                description = "View and manage available remote commands",
                onClick = onOpenCommands
            )

            ActionCard(
                title = "Transport Channels",
                icon = Icons.Default.Send,
                description = "Configure SMS, Notification, and Web transports",
                onClick = onOpenTransports
            )

            ActionCard(
                title = "Permission Manager",
                icon = Icons.Default.Lock,
                description = "Manage system permissions required by Veto",
                onClick = onOpenPermissions
            )

            ActionCard(
                title = "Settings",
                icon = Icons.Default.Settings,
                description = "Configure Veto application settings",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
fun DeviceStatusCard() {
    val context = LocalContext.current
    var deviceStatus by remember { mutableStateOf(getDeviceStatusString(context)) }

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
            Text(
                text = "Device Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = deviceStatus,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassmorphism()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getDeviceStatusString(ctx: Context): String {
    val model = Build.MODEL
    val osVersion = Build.VERSION.RELEASE

    val batteryLevel = Utils.getBatteryLevel(ctx)
    val batteryIntent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    val batteryStr = "$batteryLevel% ${if (isCharging) "⚡ (Charging)" else ""}"

    var btStr = "Unavailable"
    try {
        val hasBtPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }

        if (hasBtPerm) {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            btStr = if (btAdapter != null && btAdapter.isEnabled) "🔷 Enabled" else "⚪ Disabled"
        } else {
            btStr = "⚠️ Permission Required"
        }
    } catch (e: Exception) {
        btStr = "Unavailable"
    }

    var dndStr = "Off"
    try {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        dndStr = when (nm.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> "Off (Allow All)"
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "Priority Only"
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> "Alarms Only"
            NotificationManager.INTERRUPTION_FILTER_NONE -> "Total Silence"
            else -> "Off"
        }
    } catch (e: Exception) {
        dndStr = "Unavailable"
    }

    var soundStr = "Normal"
    try {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        soundStr = when (am.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "🔇 Silent"
            AudioManager.RINGER_MODE_VIBRATE -> "📳 Vibrate"
            AudioManager.RINGER_MODE_NORMAL -> "🔊 Normal"
            else -> "Normal"
        }
    } catch (e: Exception) {
        soundStr = "Unavailable"
    }

    val hasFlash = ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    val flashStr = if (hasFlash) "🔦 Available" else "N/A"

    return "📱 Model: $model (Android $osVersion)\n🔋 Battery: $batteryStr\n🔷 Bluetooth: $btStr\n🌙 DND Mode: $dndStr\n🔊 Sound Mode: $soundStr\n🔦 Flashlight: $flashStr"
}
