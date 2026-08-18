package com.neubofy.veto.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.neubofy.veto.R
import com.neubofy.veto.data.EncryptedSettingsRepository
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.VetoActivity
import com.neubofy.veto.ui.common.PasswordSetDialog
import com.neubofy.veto.utils.CypherUtils
import com.neubofy.veto.utils.DashboardSync
import com.neubofy.veto.utils.GoogleDriveUploader
import com.neubofy.veto.utils.MediaStorageManager
import com.neubofy.veto.utils.log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject

class AccountActivity : VetoActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var tvStatus: TextView
    private lateinit var pbSpinner: android.widget.ProgressBar

    private lateinit var layoutLogin: LinearLayout
    private lateinit var layoutLoggedIn: LinearLayout
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var btnOpenWebsite: MaterialButton
    private lateinit var btnClearCache: MaterialButton
    private lateinit var btnSignOut: MaterialButton
    private lateinit var btnDeleteData: MaterialButton
    private lateinit var btnDeleteAccount: MaterialButton

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var ivUserProfile: ImageView

    private lateinit var tvFcmStatus: TextView
    private lateinit var btnFixFcm: Button

    private lateinit var tvDriveStatus: TextView
    private lateinit var btnFixDrive: Button


    companion object {
        private val TAG = AccountActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        // Bind Views
        tvStatus = findViewById(R.id.tvConnectionStatus)
        pbSpinner = findViewById(R.id.pbConnectionSpinner)
        
        layoutLogin = findViewById(R.id.layoutLogin)
        layoutLoggedIn = findViewById(R.id.layoutLoggedIn)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnOpenWebsite = findViewById(R.id.btnOpenWebsite)
        btnClearCache = findViewById(R.id.btnClearCache)
        btnSignOut = findViewById(R.id.btnSignOut)
        btnDeleteData = findViewById(R.id.btnDeleteData)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)

        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        ivUserProfile = findViewById(R.id.ivUserProfile)

        tvFcmStatus = findViewById(R.id.tvFcmStatus)
        btnFixFcm = findViewById(R.id.btnFixFcm)

        tvDriveStatus = findViewById(R.id.tvDriveStatus)
        btnFixDrive = findViewById(R.id.btnFixDrive)


        auth = FirebaseAuth.getInstance()
        val settings = SettingsRepository.getInstance(this)
        var currentUrl = settings.get(Settings.SET_VetoSERVER_URL) as String
        if (currentUrl.isEmpty()) {
            currentUrl = "https://veto.neubofy.in"
            settings.set(Settings.SET_VetoSERVER_URL, currentUrl)
        }

        // Google Sign In Setup
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    log().e(TAG, "Google sign in failed: ${e.message}")
                    Snackbar.make(btnGoogleSignIn, "Google sign in failed: ${e.message}", Snackbar.LENGTH_LONG).show()
                    tvStatus.text = "Sign in failed"
                    pbSpinner.visibility = View.GONE
                }
            } else {
                tvStatus.text = "Sign in cancelled"
                pbSpinner.visibility = View.GONE
            }
        }

        // Listeners
        btnGoogleSignIn.setOnClickListener {
            tvStatus.text = "Signing in with Google & Setting up Drive..."
            pbSpinner.visibility = View.VISIBLE
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        btnSignOut.setOnClickListener {
            performServerAction("/api/device/link", mapOf("fcmToken" to "")) {
                runOnUiThread {
                    auth.signOut()
                    googleSignInClient.signOut()
                    SettingsRepository.getInstance(this).set(Settings.SET_SYNCED_FCM_TOKEN, "")
                    updateUI()
                    Snackbar.make(btnSignOut, "Signed Out Successfully", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        btnClearCache.setOnClickListener {
            val cacheFiles = cacheDir.listFiles()
            if (cacheFiles != null && cacheFiles.isNotEmpty()) {
                var deletedCount = 0
                cacheFiles.forEach { 
                    if (it.deleteRecursively()) deletedCount++
                }
                Snackbar.make(btnClearCache, "Cleared $deletedCount cached files", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(btnClearCache, "Cache is already empty", Snackbar.LENGTH_SHORT).show()
            }
        }

        btnOpenWebsite.setOnClickListener {
            val url = SettingsRepository.getInstance(this).get(Settings.SET_VetoSERVER_URL) as String
            if (url.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }

        btnDeleteData.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear All Cloud Data")
                .setMessage("Are you sure you want to delete all cloud data associated with this device? This cannot be undone.")
                .setPositiveButton("Delete Data") { _, _ ->
                    performServerAction("/api/data/delete", mapOf("all" to true)) {
                        runOnUiThread {
                            Snackbar.make(btnDeleteData, "Cloud Data Deleted successfully", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your entire account and all associated cloud data? This cannot be undone.")
                .setPositiveButton("Delete Account") { _, _ ->
                    performServerAction("/api/user/delete", null) {
                        runOnUiThread {
                            auth.signOut()
                            googleSignInClient.signOut()
                            SettingsRepository.getInstance(this).set(Settings.SET_SYNCED_FCM_TOKEN, "")
                            updateUI()
                            Snackbar.make(btnSignOut, "Account Deleted Successfully", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        btnFixFcm.setOnClickListener {
            tvStatus.text = "Re-syncing FCM Token..."
            pbSpinner.visibility = View.VISIBLE
            DashboardSync.uploadTokenIfPaired(this) { statusMsg, _ ->
                runOnUiThread {
                    Snackbar.make(findViewById(android.R.id.content), statusMsg, Snackbar.LENGTH_LONG).show()
                    updateUI()
                }
            }
        }

        btnFixDrive.setOnClickListener {
            tvStatus.text = "Re-creating Drive folders..."
            pbSpinner.visibility = View.VISIBLE
            GoogleDriveUploader.setupDrive(this,
                onSuccess = {
                    runOnUiThread {
                        Snackbar.make(findViewById(android.R.id.content), "Drive Setup Complete!", Snackbar.LENGTH_LONG).show()
                        updateUI()
                    }
                },
                onError = { err ->
                    runOnUiThread {
                        Snackbar.make(findViewById(android.R.id.content), "Drive Error: $err", Snackbar.LENGTH_LONG).show()
                        updateUI()
                    }
                }
            )
        }


        updateUI()
    }


    private fun updateUI() {
        val user = auth.currentUser
        if (user != null) {
            // Logged In State
            layoutLogin.visibility = View.GONE
            layoutLoggedIn.visibility = View.VISIBLE
            
            tvUserName.text = user.displayName ?: "Paired User"
            tvUserEmail.text = user.email ?: "No email"

            // Check FCM Token Sync
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                var isSynced = false
                if (task.isSuccessful) {
                    val currentToken = task.result
                    val syncedToken = SettingsRepository.getInstance(this).get(Settings.SET_SYNCED_FCM_TOKEN) as String
                    isSynced = currentToken.isNotEmpty() && currentToken == syncedToken
                }
                
                if (isSynced) {
                    tvFcmStatus.text = "🟢 FCM Token: Synced"
                    btnFixFcm.visibility = View.GONE
                } else {
                    tvFcmStatus.text = "🔴 FCM Token: Sync Needed"
                    btnFixFcm.visibility = View.VISIBLE
                }
            }

            // Check Google Drive Folders
            val drivePrefs = getSharedPreferences("veto_drive_prefs", Context.MODE_PRIVATE)
            val photoF = drivePrefs.getString("drive_folder_photo", null)
            val videoF = drivePrefs.getString("drive_folder_video", null)
            val audioF = drivePrefs.getString("drive_folder_audio", null)

            if (!photoF.isNullOrBlank() && !videoF.isNullOrBlank() && !audioF.isNullOrBlank()) {
                tvDriveStatus.text = "🟢 Google Drive Folders: Configured"
                btnFixDrive.visibility = View.GONE
            } else {
                tvDriveStatus.text = "🔴 Google Drive Folders: Missing or Incomplete"
                btnFixDrive.visibility = View.VISIBLE
            }


            tvStatus.text = "Device Paired with Web Dashboard"
            pbSpinner.visibility = View.GONE
        } else {
            // Logged Out State
            layoutLogin.visibility = View.VISIBLE
            layoutLoggedIn.visibility = View.GONE
            tvStatus.text = "Device Not Connected"
            pbSpinner.visibility = View.GONE
        }
    }

    private fun onAuthSuccess() {
        val user = auth.currentUser
        if (user != null) {
            val encSettings = EncryptedSettingsRepository.getInstance(this)
            val currentPin = encSettings.getVetoPin()

            if (currentPin.isNullOrBlank()) {
                tvStatus.text = "Awaiting PIN Setup..."
                pbSpinner.visibility = View.GONE

                PasswordSetDialog.showPasswordSetDialog(
                    context = this,
                    title = "Set Required Veto PIN",
                    positiveButtonText = "Set PIN & Complete Setup",
                    message = "A PIN is required to securely encrypt your end-to-end data before pairing.",
                    minLength = 1,
                    onSuccess = { pin ->
                        val hashedPin = CypherUtils.hashPasswordForVetoPin(pin)
                        encSettings.setVetoPin(hashedPin)
                        encSettings.setRawVetoPin(pin)
                        continueAuthSetup()
                    }
                )
            } else {
                continueAuthSetup()
            }
        }
    }

    private fun continueAuthSetup() {
        tvStatus.text = "Setting up Google Drive folders..."
        pbSpinner.visibility = View.VISIBLE
        GoogleDriveUploader.setupDrive(this,
            onSuccess = {
                runOnUiThread {
                    tvStatus.text = "Drive Setup Complete. Syncing FCM Token..."
                    DashboardSync.uploadTokenIfPaired(this) { statusMsg, _ ->
                        runOnUiThread {
                            Snackbar.make(findViewById(android.R.id.content), statusMsg, Snackbar.LENGTH_LONG).show()
                            updateUI()
                        }
                    }
                    Snackbar.make(btnGoogleSignIn, "Paired & Drive Configured!", Snackbar.LENGTH_LONG).show()
                }
            },
            onError = { error ->
                runOnUiThread {
                    tvStatus.text = "Drive Setup Failed: $error"
                    Snackbar.make(btnGoogleSignIn, "Drive Setup Failed: $error", Snackbar.LENGTH_LONG).show()
                    updateUI()
                }
            }
        )
    }

    private fun performServerAction(endpoint: String, payload: Map<String, Any>? = null, onSuccess: () -> Unit) {
        val user = auth.currentUser ?: return
        tvStatus.text = "Processing request..."
        pbSpinner.visibility = View.VISIBLE

        user.getIdToken(true).addOnCompleteListener { task ->
            if (task.isSuccessful && task.result?.token != null) {
                val token = task.result?.token!!
                val dashboardUrl = SettingsRepository.getInstance(this).get(Settings.SET_VetoSERVER_URL) as String

                Thread {
                    try {
                        val apiUrl = if (dashboardUrl.endsWith("/")) "${dashboardUrl}${endpoint.removePrefix("/")}" else "$dashboardUrl$endpoint"
                        val url = URL(apiUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.setRequestProperty("Authorization", "Bearer $token")
                        connection.doOutput = true

                        val jsonParam = JSONObject()
                        jsonParam.put("token", token)
                        if (payload != null) {
                            for ((key, value) in payload) {
                                jsonParam.put(key, value)
                            }
                        }

                        val out = java.io.OutputStreamWriter(connection.outputStream)
                        out.write(jsonParam.toString())
                        out.close()

                        val responseCode = connection.responseCode
                        runOnUiThread {
                            pbSpinner.visibility = View.GONE
                            if (responseCode in 200..299) {
                                onSuccess()
                            } else {
                                Snackbar.make(findViewById(android.R.id.content), "Server Error: $responseCode", Snackbar.LENGTH_LONG).show()
                                updateUI()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            pbSpinner.visibility = View.GONE
                            Snackbar.make(findViewById(android.R.id.content), "Network Error: ${e.message}", Snackbar.LENGTH_LONG).show()
                            updateUI()
                        }
                    }
                }.start()
            } else {
                runOnUiThread {
                    pbSpinner.visibility = View.GONE
                    Snackbar.make(findViewById(android.R.id.content), "Failed to authenticate.", Snackbar.LENGTH_SHORT).show()
                    updateUI()
                }
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        tvStatus.text = "Authenticating with Firebase..."
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    onAuthSuccess()
                } else {
                    val e = task.exception
                    log().w(TAG, "Firebase sign in failed: ${e?.message}")
                    Snackbar.make(btnGoogleSignIn, "Firebase Authentication Failed: ${e?.message}", Snackbar.LENGTH_LONG).show()
                    tvStatus.text = "Firebase Auth Failed"
                    pbSpinner.visibility = View.GONE
                }
            }
    }
}
