package com.neubofy.veto.ui

import android.content.Intent
import android.os.Bundle
import com.google.android.material.appbar.MaterialToolbar
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.services.TempContactExpiredService
import com.neubofy.veto.ui.home.MainPageFragment
import com.neubofy.veto.ui.home.PermissionManagerFragment
import com.neubofy.veto.ui.settings.LogViewActivity
import com.neubofy.veto.utils.Notifications
import com.neubofy.veto.utils.UpdateManager

class MainActivity : VetoActivity() {

    private lateinit var settings: SettingsRepository

    companion object {
        const val EXTRA_OPEN_FRAGMENT = "EXTRA_OPEN_FRAGMENT"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null && "PERMISSIONS" == intent.getStringExtra(EXTRA_OPEN_FRAGMENT)) {
            val highlightName = intent.getIntExtra(PermissionManagerFragment.ARG_HIGHLIGHT_PERMISSION_NAME, -1)
            val fragment = PermissionManagerFragment()
            if (highlightName != -1) {
                val args = Bundle().apply {
                    putInt(PermissionManagerFragment.ARG_HIGHLIGHT_PERMISSION_NAME, highlightName)
                }
                fragment.arguments = args
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        UiUtil.setupEdgeToEdgeAppBar(findViewById(R.id.appBar))

        settings = SettingsRepository.getInstance(this)
        settings.load()

        if (settings.get(Settings.SET_APP_CRASHED_LOG_ENTRY) == 1) {
            settings.set(Settings.SET_APP_CRASHED_LOG_ENTRY, 0)
            Notifications.notify(
                this,
                "Veto Background Recovery",
                "App recovered from an uncaught background crash. Log entry saved in Log View.",
                Notifications.CHANNEL_FAILED,
                LogViewActivity::class.java
            )
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MainPageFragment())
                .commit()
            handleIntent(getIntent())
        }

        // Silently check for updates
        UpdateManager.checkForUpdates(this, silent = true, isBeta = false, onCheckComplete = null)
    }

    override fun onResume() {
        super.onResume()
        TempContactExpiredService.scheduleJob(this, 0)
    }
}
