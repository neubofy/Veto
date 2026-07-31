package com.neubofy.veto.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.DynamicColors
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository


abstract class VetoActivity : AppCompatActivity() {

    private lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settings = SettingsRepository.getInstance(this)

        applyTheme()
        applyDynamicColors()

        // Needs to be called before setContentView.
        // Thus children need to call super.onCreate before setContentView.
        // Needs to be after dynamic colors.
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
    }

    override fun onResume() {
        super.onResume()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
        }
    }

    fun applyTheme() {
        val theme = settings.get(Settings.SET_THEME) as String

        val nightMode = if (theme == Settings.VAL_THEME_LIGHT) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else if (theme == Settings.VAL_THEME_DARK) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun applyDynamicColors() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }
        val isEnabled = settings.get(Settings.SET_DYNAMIC_COLORS) as Boolean
        if (isEnabled) {
            DynamicColors.applyToActivityIfAvailable(this)
        }
    }
}
