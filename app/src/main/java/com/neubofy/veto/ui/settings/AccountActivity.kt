package com.neubofy.veto.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.neubofy.veto.ui.VetoActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.theme.VetoTheme
import com.neubofy.veto.ui.theme.glassmorphism
import com.neubofy.veto.utils.DashboardSync
import com.neubofy.veto.utils.GoogleDriveUploader
import com.neubofy.veto.utils.log

class AccountActivity : VetoActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var settings: SettingsRepository

    private var _statusMessage = mutableStateOf("Device Not Connected")
    private var _isLoggedIn = mutableStateOf(false)
    private var _userName = mutableStateOf("")
    private var _userEmail = mutableStateOf("")

    private var _fcmSynced = mutableStateOf(false)
    private var _driveConfigured = mutableStateOf(false)

    companion object {
        private val TAG = AccountActivity::class.java.simpleName
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                log().e(TAG, "Google sign in failed: ${e.message}")
                _statusMessage.value = "Sign in failed: ${e.message}"
            }
        } else {
            _statusMessage.value = "Sign in cancelled"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        settings = SettingsRepository.getInstance(this)

        var currentUrl = settings.get(Settings.SET_VetoSERVER_URL) as String
        if (currentUrl.isEmpty()) {
            currentUrl = "https://veto.neubofy.in"
            settings.set(Settings.SET_VetoSERVER_URL, currentUrl)
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        updateState()

        setContent {
            VetoTheme {
                AccountScreen(
                    settings = settings,
                    isLoggedIn = _isLoggedIn.value,
                    statusMessage = _statusMessage.value,
                    userName = _userName.value,
                    userEmail = _userEmail.value,
                    fcmSynced = _fcmSynced.value,
                    driveConfigured = _driveConfigured.value,
                    serverUrl = currentUrl,
                    onGoogleSignInClick = { url ->
                        settings.set(Settings.SET_VetoSERVER_URL, url)
                        _statusMessage.value = "Signing in with Google..."
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    onSignOutClick = {
                        auth.signOut()
                        googleSignInClient.signOut()
                        settings.set(Settings.SET_VetoSERVER_ID, "")
                        settings.set(Settings.SET_SYNCED_FCM_TOKEN, "")
                        updateState()
                        Toast.makeText(this, "Signed Out", Toast.LENGTH_SHORT).show()
                    },
                    onClearCacheClick = {
                        val cacheFiles = cacheDir.listFiles()
                        if (cacheFiles != null && cacheFiles.isNotEmpty()) {
                            var deletedCount = 0
                            cacheFiles.forEach { if (it.deleteRecursively()) deletedCount++ }
                            Toast.makeText(this, "Cleared $deletedCount cached files", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Cache is already empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFixFcmClick = {
                        _statusMessage.value = "Re-syncing FCM Token..."
                        DashboardSync.uploadTokenIfPaired(this) { statusMsg, _ ->
                            runOnUiThread {
                                _statusMessage.value = statusMsg
                                updateState()
                            }
                        }
                    },
                    onFixDriveClick = {
                        _statusMessage.value = "Re-creating Drive folders..."
                        GoogleDriveUploader.setupDrive(this,
                            onSuccess = {
                                runOnUiThread {
                                    _statusMessage.value = "Drive Setup Complete!"
                                    updateState()
                                }
                            },
                            onError = { err ->
                                runOnUiThread {
                                    _statusMessage.value = "Drive Error: $err"
                                    updateState()
                                }
                            }
                        )
                    },
                    onBackClick = { finish() }
                )
            }
        }
    }

    private fun updateState() {
        val user = auth.currentUser
        if (user != null) {
            _isLoggedIn.value = true
            _userName.value = user.displayName ?: "Paired User"
            _userEmail.value = user.email ?: "No email"
            _statusMessage.value = "Device Paired with Web Dashboard"

            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentToken = task.result
                    val syncedToken = settings.get(Settings.SET_SYNCED_FCM_TOKEN) as String
                    _fcmSynced.value = currentToken.isNotEmpty() && currentToken == syncedToken
                } else {
                    _fcmSynced.value = false
                }
            }

            val drivePrefs = getSharedPreferences("veto_drive_prefs", Context.MODE_PRIVATE)
            val photoF = drivePrefs.getString("drive_folder_photo", null)
            val videoF = drivePrefs.getString("drive_folder_video", null)
            val audioF = drivePrefs.getString("drive_folder_audio", null)
            _driveConfigured.value = !photoF.isNullOrBlank() && !videoF.isNullOrBlank() && !audioF.isNullOrBlank()

        } else {
            _isLoggedIn.value = false
            _userName.value = ""
            _userEmail.value = ""
            _statusMessage.value = "Device Not Connected"
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        _statusMessage.value = "Authenticating with Firebase..."
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        settings.set(Settings.SET_VetoSERVER_ID, user.uid)
                        _statusMessage.value = "Setting up Google Drive folders..."

                        GoogleDriveUploader.setupDrive(this,
                            onSuccess = {
                                runOnUiThread {
                                    _statusMessage.value = "Drive Setup Complete. Syncing FCM..."
                                    DashboardSync.uploadTokenIfPaired(this) { statusMsg, _ ->
                                        runOnUiThread {
                                            _statusMessage.value = statusMsg
                                            updateState()
                                            Toast.makeText(this, "Paired Successfully!", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            onError = { err ->
                                runOnUiThread {
                                    _statusMessage.value = "Drive Error: $err"
                                    Toast.makeText(this, "Drive Setup Failed: $err", Toast.LENGTH_LONG).show()
                                    updateState()
                                }
                            }
                        )
                    }
                } else {
                    _statusMessage.value = "Firebase Auth Failed: ${task.exception?.message}"
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    settings: SettingsRepository,
    isLoggedIn: Boolean,
    statusMessage: String,
    userName: String,
    userEmail: String,
    fcmSynced: Boolean,
    driveConfigured: Boolean,
    serverUrl: String,
    onGoogleSignInClick: (String) -> Unit,
    onSignOutClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onFixFcmClick: () -> Unit,
    onFixDriveClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var urlInput by remember { mutableStateOf(serverUrl) }
    var showAdvanced by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Setup", fontWeight = FontWeight.Bold) },
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

            // Header Image/Icon
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (!isLoggedIn) {
                // Login View
                Card(
                    modifier = Modifier.fillMaxWidth().glassmorphism(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Connect Device",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Sign in to pair this device with the Veto Web Dashboard and configure Google Drive backups.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onGoogleSignInClick(urlInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign In with Google & Pair")
                        }
                    }
                }
            } else {
                // Logged In View
                Card(
                    modifier = Modifier.fillMaxWidth().glassmorphism(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().glassmorphism(),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("System & Storage Health", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))

                        // FCM Status
                        Text(
                            if (fcmSynced) "🟢 FCM Token: Synced" else "🔴 FCM Token: Sync Needed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!fcmSynced) {
                            OutlinedButton(onClick = onFixFcmClick, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Re-Sync Token")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Drive Status
                        Text(
                            if (driveConfigured) "🟢 Google Drive Folders: Configured" else "🔴 Google Drive Folders: Missing or Incomplete",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (!driveConfigured) {
                            OutlinedButton(onClick = onFixDriveClick, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Re-Create Drive Folders")
                            }
                        }
                    }
                }

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val url = urlInput.trim()
                            if (url.isNotEmpty()) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: Exception) {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Web Dashboard")
                    }

                    OutlinedButton(onClick = onClearCacheClick, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear Local Cache")
                    }

                    OutlinedButton(
                        onClick = onSignOutClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Sign Out Account")
                    }
                }
            }

            Text(
                text = statusMessage,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Advanced settings
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text("Server URL Settings ⚙️")
                }

                if (showAdvanced) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Dashboard Server URL") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
