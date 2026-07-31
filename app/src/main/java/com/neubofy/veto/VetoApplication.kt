package com.neubofy.veto

import android.app.Application
import android.content.Context
import android.service.notification.StatusBarNotification
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.data.UncaughtExceptionHandler.Companion.initUncaughtExceptionHandler
import com.neubofy.veto.ui.onboarding.PinUpdate
import com.neubofy.veto.ui.onboarding.UpdateboardingModernCryptoActivity
import com.neubofy.veto.utils.Notifications
import com.neubofy.veto.utils.log


class VetoApplication : Application() {

    companion object {
        private val TAG = VetoApplication::class.java.simpleName
    }

    // Workaround to "pass" this from the NotificationListenerService to the CommandExecutionWorker.
    // The problem is that we cannot pass objects between them directly.
    // But we also cannot retrieve the notification in the worker by ID,
    // because notificationManager.activeNotifications only returns the notifications posted by our own app.    
    var latestStatusBarNotification: StatusBarNotification? = null

    override fun onCreate() {
        super.onCreate()

        this.log().i(TAG, "Starting VetoApplication")

        Notifications.init(this)
        initUncaughtExceptionHandler(this)

        doUpdateMigrations(this)

        restartServices()
    }

    private fun doUpdateMigrations(context: Context) {
        val settings = SettingsRepository.getInstance(context)
        settings.migrateSettings()
        UpdateboardingModernCryptoActivity.notifyAboutCryptoRefreshIfRequired(context)
        PinUpdate.migratePin(context)
    }

    fun restartServices() {
        val settings = SettingsRepository.getInstance(this)
        if (settings.serverAccountExists()) {
            // Background location is strictly managed by AutoLocCommand via WorkManager now.
        }
    }
}
