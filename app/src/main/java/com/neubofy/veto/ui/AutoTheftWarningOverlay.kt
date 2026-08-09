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
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.utils.AutoTheftManager

class AutoTheftWarningOverlay : VetoActivity() {

    private lateinit var settings: SettingsRepository

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

        val textViewInstructions = findViewById<TextView>(R.id.textViewInstructions)
        textViewInstructions.text = "To prove you are the legitimate owner, please unlock this device."

        val buttonUnlock = findViewById<Button>(R.id.buttonUnlock)
        buttonUnlock.setOnClickListener {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        super.onDismissSucceeded()
                        // Proof by unlock successful!
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
