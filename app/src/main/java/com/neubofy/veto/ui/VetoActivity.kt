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

    private lateinit var baseSettings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseSettings = SettingsRepository.getInstance(this)
        try {
            baseSettings.load()
        } catch (_: Exception) {}

        applyTheme()
        applyDynamicColors()

        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            window.isNavigationBarContrastEnforced = false
            @Suppress("DEPRECATION")
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
        try {
            val theme = baseSettings.get(Settings.SET_THEME) as? String ?: Settings.VAL_THEME_FOLLOW_SYSTEM

            val nightMode = if (theme == Settings.VAL_THEME_LIGHT) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else if (theme == Settings.VAL_THEME_DARK) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
        } catch (_: Exception) {}
    }

    fun applyDynamicColors() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }
        try {
            val isEnabled = baseSettings.get(Settings.SET_DYNAMIC_COLORS) as? Boolean ?: false
            if (isEnabled) {
                DynamicColors.applyToActivityIfAvailable(this)
            }
        } catch (_: Exception) {}
    }
}
