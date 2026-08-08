package com.neubofy.veto.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.services.TempContactExpiredService
import com.neubofy.veto.ui.home.MainPageScreen
import com.neubofy.veto.ui.home.TransportListScreen
import com.neubofy.veto.ui.home.PermissionManagerScreen
import com.neubofy.veto.ui.settings.LogViewActivity
import com.neubofy.veto.ui.theme.VetoTheme
import com.neubofy.veto.utils.Notifications
import com.neubofy.veto.utils.UpdateManager
import com.neubofy.veto.transports.availableTransports
import com.neubofy.veto.permissions.globalAppPermissions

class MainActivity : VetoActivity() {

    private lateinit var settings: SettingsRepository

    companion object {
        const val EXTRA_OPEN_FRAGMENT = "EXTRA_OPEN_FRAGMENT"
    }

    // A simple hack to expose the required highlighted permission for deep linking
    private var initialPermissionHighlightName: Int = -1
    private var isNavigationPending = androidx.compose.runtime.mutableStateOf(false)
    private var pendingDestination = "main"

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null && "PERMISSIONS" == intent.getStringExtra(EXTRA_OPEN_FRAGMENT)) {
            // ARG_HIGHLIGHT_PERMISSION_NAME equivalent handling
            // The legacy system used the string literal "ARG_HIGHLIGHT_PERMISSION_NAME"
            initialPermissionHighlightName = intent.getIntExtra("ARG_HIGHLIGHT_PERMISSION_NAME", -1)
            pendingDestination = "permissions"
            isNavigationPending.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        handleIntent(intent)

        setContent {
            VetoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Handle intent deep linking manually on launch
                    androidx.compose.runtime.LaunchedEffect(isNavigationPending.value) {
                        if (isNavigationPending.value) {
                            navController.navigate(pendingDestination) {
                                popUpTo("main") { inclusive = false }
                            }
                            isNavigationPending.value = false
                        }
                    }

                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainPageScreen(
                                onOpenCommands = {
                                    startActivity(Intent(this@MainActivity, com.neubofy.veto.ui.settings.CommandsActivity::class.java))
                                },
                                onOpenSettings = {
                                    startActivity(Intent(this@MainActivity, com.neubofy.veto.ui.settings.SettingsActivity::class.java))
                                },
                                onOpenPermissions = {
                                    navController.navigate("permissions")
                                },
                                onOpenTransports = {
                                    navController.navigate("transports")
                                }
                            )
                        }

                        composable("transports") {
                            TransportListScreen(
                                transports = availableTransports(this@MainActivity),
                                activity = this@MainActivity
                            )
                        }

                        composable("permissions") {
                            PermissionManagerScreen(
                                permissions = globalAppPermissions(),
                                activity = this@MainActivity,
                                highlightName = initialPermissionHighlightName
                            )
                        }
                    }
                }
            }
        }

        // Silently check for updates
        UpdateManager.checkForUpdates(this, silent = true, isBeta = false, onCheckComplete = null)
    }

    override fun onResume() {
        super.onResume()
        TempContactExpiredService.scheduleJob(this, 0)
    }
}
