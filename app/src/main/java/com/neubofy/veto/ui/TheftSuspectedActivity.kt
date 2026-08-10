package com.neubofy.veto.ui

import android.app.AlarmManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TheftSuspectedActivity : VetoActivity() {

    private lateinit var settings: SettingsRepository
    private var countDownTimer: android.os.CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_theft_suspected)

        settings = SettingsRepository.getInstance(this)

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

        countDownTimer = object : android.os.CountDownTimer(3 * 60 * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // UI shows radar animation, no explicit text update needed
            }

            override fun onFinish() {
                confirmTheftMode()
            }
        }.start()

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
        
        // Also listen for USER_PRESENT (device unlocked)
        val unlockFilter = IntentFilter(Intent.ACTION_USER_PRESENT)
        val unlockReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_USER_PRESENT) {
                    endTheftMode()
                }
            }
        }
        registerReceiver(unlockReceiver, unlockFilter)

        if (intent.getBooleanExtra("isConfirmed", false)) {
            countDownTimer?.cancel()
            confirmTheftMode()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("isConfirmed", false)) {
            countDownTimer?.cancel()
            confirmTheftMode()
        }
    }

    private fun endTheftMode() {
        settings.set(Settings.SET_THEFT_MODE_ACTIVE, false)
        
        countDownTimer?.cancel()

        // Cancel the backup alarm if it hasn't fired yet
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, com.neubofy.veto.receiver.TheftConfirmedReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        finish()
    }

    private fun confirmTheftMode() {
        if (settings.get(Settings.SET_THEFT_MODE_ACTIVE) as Boolean) {
            val serviceIntent = Intent(this, com.neubofy.veto.services.TheftModeService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)

            // Transition UI to terrifying confirmed state in-place
            val mainLayout = findViewById<View>(android.R.id.content)
            mainLayout.setBackgroundColor(android.graphics.Color.parseColor("#0A0000")) // Scary dark red/black
            
            val titleText = findViewById<TextView>(R.id.titleText)
            titleText.text = "VETO: THEFT CONFIRMED"
            titleText.setTextColor(android.graphics.Color.parseColor("#FF0000"))

            val radarStatusText = findViewById<TextView>(R.id.radarStatusText)
            radarStatusText.text = "Connected via Mesh Network"
            radarStatusText.setTextColor(android.graphics.Color.parseColor("#00FF00"))
            
            val lockMessage = findViewById<TextView>(R.id.textViewLockScreenMessage)
            lockMessage.setTextColor(android.graphics.Color.WHITE)
            
            val contactInfo = findViewById<TextView>(R.id.textViewTheftContact)
            contactInfo.setTextColor(android.graphics.Color.WHITE)
            
            // Swap radar for network diagram
            val radarScanView = findViewById<View>(R.id.radarScanView)
            radarScanView.visibility = View.GONE
            
            val networkIcon = findViewById<ImageView>(R.id.networkIcon)
            networkIcon.visibility = View.VISIBLE
            
            // Show terminal
            val terminalScrollView = findViewById<ScrollView>(R.id.terminalScrollView)
            terminalScrollView.visibility = View.VISIBLE
            
            val terminalLogText = findViewById<TextView>(R.id.terminalLogText)
            startFakeTerminalLogs(terminalLogText, terminalScrollView)
        }
    }

    private fun startFakeTerminalLogs(textView: TextView, scrollView: ScrollView) {
        lifecycleScope.launch {
            val logs = listOf(
                "Initializing Veto Anti-Theft Protocol...",
                "Bypassing local restrictions...",
                "Establishing secure connection to Veto Mesh Network [SUCCESS]",
                "Acquiring GPS coordinates...",
                "Coordinates locked: LAT 40.7128, LNG -74.0060 (precision: 2m)",
                "Uploading coordinates to secure cloud...",
                "Activating stealth camera...",
                "Capturing front camera image...",
                "Image captured and encrypted. Uploading...",
                "Uploading audio snippet (10s)...",
                "Locking hardware identifiers (IMEI/MAC)...",
                "Broadcasting device state to network nodes...",
                "Waiting for owner's remote command...",
                "Analyzing network traffic for anomalies...",
                "Device lockdown active. Recovery impossible without master key."
            )
            val sb = java.lang.StringBuilder()
            for (log in logs) {
                sb.append("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}] ")
                
                // Typewriter effect
                for (char in log) {
                    sb.append(char)
                    textView.text = sb.toString()
                    scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                    delay((10..50).random().toLong())
                }
                sb.append("\n")
                textView.text = sb.toString()
                scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                delay((500..2000).random().toLong())
            }
            
            // Continuous loop
            while (true) {
                val ping = "[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}] Heartbeat ping... 32 bytes from 192.168.x.x time=14ms\n"
                sb.append(ping)
                if (sb.length > 2000) {
                    sb.delete(0, sb.length - 1500)
                }
                textView.text = sb.toString()
                scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
                delay(3000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km?.isKeyguardLocked == true) {
            val reorderIntent = Intent(this, TheftSuspectedActivity::class.java).apply {
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
                val reorderIntent = Intent(this, TheftSuspectedActivity::class.java).apply {
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
