package com.neubofy.veto.ui

import android.app.NotificationManager
import android.app.NotificationManager.INTERRUPTION_FILTER_ALL
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import com.neubofy.veto.R
import com.neubofy.veto.commands.RING_DURATION_DEFAULT_SECS
import com.neubofy.veto.commands.RING_DURATION_MAX_SECS
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.receiver.DeviceAdminReceiver
import com.neubofy.veto.services.RingerService
import com.neubofy.veto.utils.RingerUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


const val EXTRA_RING_DURATION: String = "EXTRA_RING_DURATION"

class RingerActivity : VetoActivity() {

    companion object {
        fun newInstance(context: Context, duration: Int) {
            val intent = Intent(context, RingerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_RING_DURATION, duration)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ring)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val bundle = intent.extras
        var durationSec: Int = bundle?.getInt(EXTRA_RING_DURATION) ?: RING_DURATION_DEFAULT_SECS
        if (durationSec > RING_DURATION_MAX_SECS) {
            durationSec = RING_DURATION_MAX_SECS
        }

        // Start persistent RingerService (Audio ringing + 100% volume reset until ACTION_USER_PRESENT / unlocked)
        RingerService.startRinging(this, durationSec)

        val buttonStopRinging = findViewById<Button>(R.id.buttonStopRinging)
        buttonStopRinging.setOnClickListener {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
            if (km == null) {
                stopAndFinish()
                return@setOnClickListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() {
                        super.onDismissSucceeded()
                        stopAndFinish()
                    }
                })
            } else {
                if (km.isKeyguardSecure) {
                    val authIntent = km.createConfirmDeviceCredentialIntent(null, null)
                    if (authIntent != null) {
                        startActivityForResult(authIntent, 100)
                    } else {
                        stopAndFinish()
                    }
                } else {
                    stopAndFinish()
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
        if (km?.isKeyguardLocked == true) {
            val reorderIntent = Intent(this, RingerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(reorderIntent)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager?
            if (km?.isKeyguardLocked == true) {
                val reorderIntent = Intent(this, RingerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(reorderIntent)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            stopAndFinish()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
            keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun stopAndFinish() {
        val settings = SettingsRepository.getInstance(this)
        settings.set(Settings.SET_THEFT_MODE_ACTIVE, false)
        RingerService.stopRinging(this)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
