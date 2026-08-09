package com.neubofy.veto.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.utils.AutoTheftDefenseManager
import com.neubofy.veto.utils.AutoTheftManager
import kotlinx.coroutines.*

class AutoTheftWarningOverlay : VetoActivity() {

    private lateinit var settings: SettingsRepository
    private val overlayScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var countdownJob: Job? = null

    companion object {
        const val REASON_TEXT = "REASON_TEXT"
        const val LOCK_MSG_TEXT = "LOCK_MSG_TEXT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_auto_theft_warning)

        settings = SettingsRepository.getInstance(this)
        settings.load()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Block back button
            }
        })

        val reasonText = intent.getStringExtra(REASON_TEXT)
        val lockMsgText = intent.getStringExtra(LOCK_MSG_TEXT)

        val textViewWarning = findViewById<TextView>(R.id.textViewWarningMessage)
        if (!reasonText.isNullOrEmpty()) {
            var msg = "Theft suspected: $reasonText"
            if (!lockMsgText.isNullOrEmpty()) {
                msg += "\n\nMessage: \"$lockMsgText\""
            }
            textViewWarning.text = msg
        } else if (!lockMsgText.isNullOrEmpty()) {
            textViewWarning.text = "Message: \"$lockMsgText\""
        }

        // Setup Owner Contact Card if details exist
        val phone = settings.get(Settings.SET_AUTO_THEFT_CONTACT_PHONE) as? String ?: ""
        val email = settings.get(Settings.SET_AUTO_THEFT_CONTACT_EMAIL) as? String ?: ""
        val social = settings.get(Settings.SET_AUTO_THEFT_CONTACT_SOCIAL) as? String ?: ""

        val cardOwnerContact = findViewById<LinearLayout>(R.id.cardOwnerContact)
        val tvContactDetails = findViewById<TextView>(R.id.tvContactDetails)

        if (phone.isNotEmpty() || email.isNotEmpty() || social.isNotEmpty()) {
            cardOwnerContact.visibility = View.VISIBLE
            val sb = StringBuilder("If found, please contact owner or submit to police station:\n")
            if (phone.isNotEmpty()) sb.append("• Phone: $phone\n")
            if (email.isNotEmpty()) sb.append("• Email: $email\n")
            if (social.isNotEmpty()) sb.append("• Social ID: $social")
            tvContactDetails.text = sb.toString().trim()
        }

        // Setup Live Terminal Console Stream
        val tvTerminalLogs = findViewById<TextView>(R.id.tvTerminalLogs)
        val scrollViewTerminal = findViewById<ScrollView>(R.id.scrollViewTerminal)

        AutoTheftDefenseManager.setLogListener {
            runOnUiThread {
                val logs = AutoTheftDefenseManager.getLogs()
                tvTerminalLogs.text = logs.joinToString("\n")
                scrollViewTerminal.post { scrollViewTerminal.fullScroll(View.FOCUS_DOWN) }
            }
        }

        // Start 3-minute Grace Countdown & Progress Bar (180 seconds)
        val tvGraceCountdown = findViewById<TextView>(R.id.tvGraceCountdown)
        val progressBarGraceTimer = findViewById<ProgressBar>(R.id.progressBarGraceTimer)

        startCountdownTimer(tvGraceCountdown, progressBarGraceTimer)

        val buttonUnlock = findViewById<Button>(R.id.buttonUnlock)
        buttonUnlock.setOnClickListener {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        super.onDismissSucceeded()
                        AutoTheftManager.cancelSuspectedMode(this@AutoTheftWarningOverlay)
                        finish()
                    }

                    override fun onDismissError() {
                        super.onDismissError()
                        finish()
                    }
                })
            } else {
                finish()
            }
        }

        hideSystemUI()
    }

    private fun startCountdownTimer(tvCountdown: TextView, progressBar: ProgressBar) {
        countdownJob?.cancel()
        countdownJob = overlayScope.launch {
            for (secs in 0..180) {
                val remaining = 180 - secs
                val minutes = remaining / 60
                val seconds = remaining % 60
                tvCountdown.text = String.format("Theft Confirmation in: %02d:%02d", minutes, seconds)
                progressBar.progress = secs

                if (remaining == 0) {
                    tvCountdown.text = "🚨 THEFT CONFIRMED! ACTIVE DEFENSE ENGAGED"
                    tvCountdown.setTextColor(android.graphics.Color.parseColor("#ff7b72"))
                }
                delay(1000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayScope.cancel()
        AutoTheftDefenseManager.setLogListener(null)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (settings.get(Settings.SET_AUTO_THEFT_WARNING_ACTIVE) as Boolean) {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km?.isKeyguardLocked == true) {
                val reorderIntent = Intent(this, AutoTheftWarningOverlay::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(reorderIntent)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && (settings.get(Settings.SET_AUTO_THEFT_WARNING_ACTIVE) as Boolean)) {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km?.isKeyguardLocked == true) {
                val reorderIntent = Intent(this, AutoTheftWarningOverlay::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(reorderIntent)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return true
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            controller?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }
}
