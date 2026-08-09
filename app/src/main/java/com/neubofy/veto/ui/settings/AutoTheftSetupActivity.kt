package com.neubofy.veto.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                Surface(modifier = Modifier.fillMaxSize()) {
                    AutoTheftScreen(settings)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoTheftScreen(settings: SettingsRepository) {
    var isEnabled by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_ENABLED) as Boolean) }

    var simRemoved by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_SIM_REMOVED) as Boolean) }
    var failedUnlock by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_FAILED_UNLOCK) as Boolean) }
    var maxAttempts by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_MAX_ATTEMPTS) as Int) }

    var proofUnlock by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_PROOF_UNLOCK) as Boolean) }
    var proofCharge by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_PROOF_CHARGE) as Boolean) }
    var proofSim by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_PROOF_SIM) as Boolean) }

    var lockMsg by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_LOCK_MSG) as String) }
    var ownerSim by remember { mutableStateOf(settings.get(Settings.SET_AUTO_THEFT_OWNER_SIM) as String) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Auto Theft Detection Setup") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Enable Auto Theft Detection", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { it ->
                        isEnabled = it
                        settings.set(Settings.SET_AUTO_THEFT_ENABLED, it as Any)
                    }
                )
            }

            HorizontalDivider()
            Text("Detection Criteria", style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SIM Card Removed")
                Switch(checked = simRemoved, onCheckedChange = { it -> simRemoved = it; settings.set(Settings.SET_AUTO_THEFT_SIM_REMOVED, it as Any) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Max Failed Unlock Attempts")
                Switch(checked = failedUnlock, onCheckedChange = { it -> failedUnlock = it; settings.set(Settings.SET_AUTO_THEFT_FAILED_UNLOCK, it as Any) })
            }
            if (failedUnlock) {
                OutlinedTextField(
                    value = maxAttempts.toString(),
                    onValueChange = { it ->
                        val v = it.toIntOrNull() ?: 3
                        maxAttempts = v
                        settings.set(Settings.SET_AUTO_THEFT_MAX_ATTEMPTS, v as Any)
                    },
                    label = { Text("Max Attempts") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            HorizontalDivider()
            Text("Proving Legitimacy (Cancellation)", style = MaterialTheme.typography.titleMedium)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Unlock Phone")
                Switch(checked = proofUnlock, onCheckedChange = { it -> proofUnlock = it; settings.set(Settings.SET_AUTO_THEFT_PROOF_UNLOCK, it as Any) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Put on Charger")
                Switch(checked = proofCharge, onCheckedChange = { it -> proofCharge = it; settings.set(Settings.SET_AUTO_THEFT_PROOF_CHARGE, it as Any) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Reinsert Owner SIM")
                Switch(checked = proofSim, onCheckedChange = { it -> proofSim = it; settings.set(Settings.SET_AUTO_THEFT_PROOF_SIM, it as Any) })
            }

            HorizontalDivider()
            Text("Action Behaviors", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = lockMsg,
                onValueChange = { it -> lockMsg = it; settings.set(Settings.SET_AUTO_THEFT_LOCK_MSG, it as Any) },
                label = { Text("Lock Screen Message During Theft") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ownerSim,
                onValueChange = { it -> ownerSim = it; settings.set(Settings.SET_AUTO_THEFT_OWNER_SIM, it as Any) },
                label = { Text("Owner SIM Number / ID (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
