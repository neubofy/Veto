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
    
    @Volatile
    private var isDisturbActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

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
        
        // Show initial connection status (Suspected phase)
        val statusBox = findViewById<TextView>(R.id.textViewConnectionStatus)
        statusBox.visibility = View.VISIBLE
        statusBox.text = "Searching for nearby devices... Connecting..."

        val textViewContact = findViewById<TextView>(R.id.textViewTheftContact)
        val contact = settings.get(Settings.SET_THEFT_CONTACT_INFO) as String
        if (contact.isNotEmpty()) {
            textViewContact.text = contact
            textViewContact.visibility = View.VISIBLE
        } else {
            textViewContact.visibility = View.GONE
        }

        val durationMins = settings.get(Settings.SET_THEFT_SUSPECTED_DURATION) as Int
        countDownTimer = object : android.os.CountDownTimer(durationMins * 60 * 1000L, 1000) {
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
        
        // Listen for DisturbThief events
        val disturbFilter = IntentFilter().apply {
            addAction("com.neubofy.veto.ACTION_DISTURB_START")
            addAction("com.neubofy.veto.ACTION_DISTURB_STOP")
        }
        val disturbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.neubofy.veto.ACTION_DISTURB_START" -> isDisturbActive = true
                    "com.neubofy.veto.ACTION_DISTURB_STOP" -> isDisturbActive = false
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(disturbReceiver, disturbFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(disturbReceiver, disturbFilter)
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
        settings.set(Settings.SET_THEFT_MODE_CONFIRMED, false)
        
        countDownTimer?.cancel()

        // Cancel the backup alarm if it hasn't fired yet
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, com.neubofy.veto.receiver.TheftConfirmedReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        // Stop the background service and noisy alarms
        val serviceIntent = Intent(this, com.neubofy.veto.services.TheftModeService::class.java)
        stopService(serviceIntent)
        
        finish()
    }

    private fun confirmTheftMode() {
        if (settings.get(Settings.SET_THEFT_MODE_ACTIVE) as Boolean) {
            settings.set(Settings.SET_THEFT_MODE_CONFIRMED, true)
            val serviceIntent = Intent(this, com.neubofy.veto.services.TheftModeService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent)

            // Transition UI to terrifying confirmed state in-place
            val mainLayout = findViewById<View>(android.R.id.content)
            mainLayout.setBackgroundColor(android.graphics.Color.parseColor("#0A0000")) // Scary dark red/black
            
            val titleText = findViewById<TextView>(R.id.titleText)
            titleText.text = "VETO: THEFT CONFIRMED"
            titleText.setTextColor(android.graphics.Color.parseColor("#FF0000"))
            
            val lockMessage = findViewById<TextView>(R.id.textViewLockScreenMessage)
            lockMessage.setTextColor(android.graphics.Color.WHITE)
            
            val contactInfo = findViewById<TextView>(R.id.textViewTheftContact)
            contactInfo.setTextColor(android.graphics.Color.WHITE)
            
            val foundPhoneLayout = findViewById<View>(R.id.layoutFoundPhoneInstructions)
            foundPhoneLayout?.visibility = View.VISIBLE
            
            // Show static connection status
            val statusBox = findViewById<TextView>(R.id.textViewConnectionStatus)
            statusBox.visibility = View.VISIBLE
            statusBox.text = "Connected via Veto mesh network. Communicating..."
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
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
