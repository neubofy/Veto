package com.neubofy.veto.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.VetoActivity
import com.neubofy.veto.utils.DashboardSync
import com.neubofy.veto.utils.log

class AccountActivity : VetoActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var tvStatus: TextView
    private lateinit var etDashboardUrl: TextInputEditText

    private lateinit var layoutLogin: LinearLayout
    private lateinit var layoutLoggedIn: LinearLayout
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var btnOpenWebsite: MaterialButton
    private lateinit var btnSyncDevice: MaterialButton
    private lateinit var btnClearCache: MaterialButton
    private lateinit var btnSignOut: MaterialButton

    companion object {
        private val TAG = AccountActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        // Bind Views
        etDashboardUrl = findViewById(R.id.etDashboardUrl)
        tvStatus = findViewById(R.id.tvConnectionStatus)
        
        layoutLogin = findViewById(R.id.layoutLogin)
        layoutLoggedIn = findViewById(R.id.layoutLoggedIn)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnOpenWebsite = findViewById(R.id.btnOpenWebsite)
        btnSyncDevice = findViewById(R.id.btnSyncDevice)
        btnClearCache = findViewById(R.id.btnClearCache)
        btnSignOut = findViewById(R.id.btnSignOut)

        auth = FirebaseAuth.getInstance()
        val settings = SettingsRepository.getInstance(this)
        
        // Advanced Toggle Logic
        val tvAdvancedToggle = findViewById<TextView>(R.id.tvAdvancedToggle)
        val advancedLayout = findViewById<LinearLayout>(R.id.advancedLayout)
        tvAdvancedToggle.setOnClickListener {
            if (advancedLayout.visibility == View.GONE) {
                advancedLayout.visibility = View.VISIBLE
            } else {
                advancedLayout.visibility = View.GONE
            }
        }

        val currentUrl = settings.get(Settings.SET_VetoSERVER_URL) as String
        if (currentUrl.isNotEmpty()) etDashboardUrl.setText(currentUrl)

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
                }
            } else {
                tvStatus.text = "Sign in cancelled"
            }
        }

        // Listeners
        btnGoogleSignIn.setOnClickListener {
            if (!validateUrl(settings)) return@setOnClickListener
            tvStatus.text = "Signing in with Google & Setting up Drive..."
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        btnSignOut.setOnClickListener {
            auth.signOut()
            googleSignInClient.signOut()
            settings.set(Settings.SET_VetoSERVER_ID, "")
            updateUI()
            Snackbar.make(btnSignOut, "Signed Out Successfully", Snackbar.LENGTH_SHORT).show()
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
            val url = etDashboardUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }

        btnSyncDevice.setOnClickListener {
            tvStatus.text = "Syncing with Dashboard..."
            DashboardSync.uploadTokenIfPaired(this) { statusMsg, isSuccess ->
                runOnUiThread {
                    tvStatus.text = statusMsg
                }
            }
        }

        updateUI()
    }

    private fun validateUrl(settings: SettingsRepository): Boolean {
        val url = etDashboardUrl.text.toString().trim()
        if (url.isEmpty()) {
            Snackbar.make(btnGoogleSignIn, "Please enter a Dashboard URL in Advanced Settings.", Snackbar.LENGTH_SHORT).show()
            return false
        }
        settings.set(Settings.SET_VetoSERVER_URL, url)
        return true
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user != null) {
            // Logged In State
            layoutLogin.visibility = View.GONE
            layoutLoggedIn.visibility = View.VISIBLE
            val email = user.email ?: "Unknown"
            
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                var isSynced = false
                if (task.isSuccessful) {
                    val currentToken = task.result
                    val syncedToken = SettingsRepository.getInstance(this).get(Settings.SET_SYNCED_FCM_TOKEN) as String
                    isSynced = currentToken.isNotEmpty() && currentToken == syncedToken
                }
                
                if (isSynced) {
                    tvStatus.text = "Device is Paired & Synced!\nLogged in as $email"
                } else {
                    tvStatus.text = "Device Needs Syncing!\nLogged in as $email\nClick 'Sync Device to Veto'"
                }
            }
        } else {
            // Logged Out State
            layoutLogin.visibility = View.VISIBLE
            layoutLoggedIn.visibility = View.GONE
            tvStatus.text = "Not Connected"
        }
    }

    private fun onAuthSuccess() {
        val user = auth.currentUser
        if (user != null) {
            val settings = SettingsRepository.getInstance(this)
            settings.set(Settings.SET_VetoSERVER_ID, user.uid)

            tvStatus.text = "Setting up Google Drive folders..."
            com.neubofy.veto.utils.GoogleDriveUploader.setupDrive(this, 
                onSuccess = {
                    runOnUiThread {
                        tvStatus.text = "Drive Setup Complete. Syncing FCM Token..."
                        DashboardSync.uploadTokenIfPaired(this) { statusMsg, _ ->
                            runOnUiThread {
                                tvStatus.text = statusMsg
                            }
                        }
                        Snackbar.make(btnGoogleSignIn, "Paired & Drive Configured!", Snackbar.LENGTH_LONG).show()
                        updateUI()
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        tvStatus.text = "Drive Setup Failed: $error"
                        Snackbar.make(btnGoogleSignIn, "Drive Setup Failed: $error", Snackbar.LENGTH_LONG).show()
                    }
                }
            )
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
                }
            }
    }
}
