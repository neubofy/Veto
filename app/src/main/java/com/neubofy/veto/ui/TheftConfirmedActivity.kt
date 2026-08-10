package com.neubofy.veto.ui

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TheftConfirmedActivity : VetoActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var terminalLogText: TextView
    private lateinit var terminalScrollView: ScrollView
    private lateinit var radarStatusText: TextView
    private val handler = Handler(Looper.getMainLooper())
    
    private val fakeLogs = listOf(
        "Initializing tracking modules...",
        "GPS: Acquired lock (accuracy: 4m)",
        "Triangulating position via cell towers...",
        "Device fingerprint uploaded to recovery server",
        "IMEI logged and flagged as STOLEN",
        "Attempting BLE mesh connection...",
        "Mesh connection established",
        "Transmitting telemetry data...",
        "Battery status logged",
        "Network route tracing active...",
        "Microphone context sampling scheduled",
        "Awaiting further commands from owner..."
    )
    
    private var logIndex = 0
    private val logRunnable = object : Runnable {
        override fun run() {
            if (logIndex < fakeLogs.size) {
                appendLog(fakeLogs[logIndex])
                logIndex++
                handler.postDelayed(this, (1500..4000).random().toLong())
                
                if (logIndex == 6) {
                    radarStatusText.text = "Connected via BLE Mesh Network"
                }
            } else {
                // Add some repeating heartbeat logs
                appendLog("PING: Server reached, lat: ${String.format("%.4f", Math.random() * 90)}, lng: ${String.format("%.4f", Math.random() * 180)}")
                handler.postDelayed(this, 10000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_theft_confirmed)

        settings = SettingsRepository.getInstance(this)
        
        terminalLogText = findViewById(R.id.terminalLogText)
        terminalScrollView = findViewById(R.id.terminalScrollView)
        radarStatusText = findViewById(R.id.radarStatusText)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Block back button
            }
        })

        val textViewMsg = findViewById<TextView>(R.id.textViewLockScreenMessage)
        val msg = settings.get(Settings.SET_LOCKSCREEN_MESSAGE) as String
        if (msg.isNotEmpty()) {
            textViewMsg.text = msg
        }

        val textViewContact = findViewById<TextView>(R.id.textViewTheftContact)
        val contact = settings.get(Settings.SET_THEFT_CONTACT_INFO) as String
        if (contact.isNotEmpty()) {
            textViewContact.text = contact
            textViewContact.visibility = View.VISIBLE
        } else {
            textViewContact.visibility = View.GONE
        }

        val buttonUnlock = findViewById<Button>(R.id.buttonUnlock)
        buttonUnlock.setOnClickListener {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        super.onDismissSucceeded()
                        endTheftMode()
                    }

                    override fun onDismissError() {
                        super.onDismissError()
                        endTheftMode()
                    }
                })
            } else {
                endTheftMode()
            }
        }

        hideSystemUI()

        val filter = IntentFilter("com.neubofy.veto.ACTION_FINISH_LOCK_SCREEN")
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.neubofy.veto.ACTION_FINISH_LOCK_SCREEN") {
                    finish()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        
        val unlockFilter = IntentFilter(Intent.ACTION_USER_PRESENT)
        val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    endTheftMode()
                }
            }
        }
        registerReceiver(unlockReceiver, unlockFilter)
        
        // Start terminal logs
        handler.postDelayed(logRunnable, 1000)
    }
    
    private fun appendLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val currentText = terminalLogText.text.toString()
        val newText = if (currentText.isEmpty()) "[$time] $msg" else "$currentText\n[$time] $msg"
        terminalLogText.text = newText
        
        // Auto scroll to bottom
        terminalScrollView.post {
            terminalScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun endTheftMode() {
        settings.set(Settings.SET_THEFT_MODE_ACTIVE, false)
        
        // Stop service if running
        val serviceIntent = Intent(this, com.neubofy.veto.services.TheftModeService::class.java)
        stopService(serviceIntent)
        
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(logRunnable)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km?.isKeyguardLocked == true) {
            val reorderIntent = Intent(this, TheftConfirmedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(reorderIntent)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km?.isKeyguardLocked == true) {
                val reorderIntent = Intent(this, TheftConfirmedActivity::class.java).apply {
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
