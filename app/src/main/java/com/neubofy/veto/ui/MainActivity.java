package com.neubofy.veto.ui;

import static com.neubofy.veto.ui.SetupWarningsActivityKt.shouldShowSetupWarnings;
import static com.neubofy.veto.ui.UiUtil.setupEdgeToEdgeAppBar;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.neubofy.veto.BuildConfig;
import com.neubofy.veto.R;
import com.neubofy.veto.data.Settings;
import com.neubofy.veto.data.SettingsRepository;
import com.neubofy.veto.services.TempContactExpiredService;
import com.neubofy.veto.ui.home.MainPageFragment;

import com.neubofy.veto.ui.settings.AboutActivity;
import com.neubofy.veto.utils.UpdateManager;
import kotlin.Unit;

public class MainActivity extends VetoActivity {

    SettingsRepository settings;

    public static final String EXTRA_OPEN_FRAGMENT = "EXTRA_OPEN_FRAGMENT";

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && "PERMISSIONS".equals(intent.getStringExtra(EXTRA_OPEN_FRAGMENT))) {
            int highlightName = intent.getIntExtra(com.neubofy.veto.ui.home.PermissionManagerFragment.ARG_HIGHLIGHT_PERMISSION_NAME, -1);
            com.neubofy.veto.ui.home.PermissionManagerFragment fragment = new com.neubofy.veto.ui.home.PermissionManagerFragment();
            if (highlightName != -1) {
                Bundle args = new Bundle();
                args.putInt(com.neubofy.veto.ui.home.PermissionManagerFragment.ARG_HIGHLIGHT_PERMISSION_NAME, highlightName);
                fragment.setArguments(args);
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));

        settings = SettingsRepository.Companion.getInstance(this);
        settings.load();

        if (((Integer) settings.get(Settings.SET_APP_CRASHED_LOG_ENTRY)) == 1) {
            settings.set(Settings.SET_APP_CRASHED_LOG_ENTRY, 0);
            com.neubofy.veto.utils.Notifications.notify(
                this,
                "Veto Background Recovery",
                "App recovered from an uncaught background crash. Log entry saved in Log View.",
                com.neubofy.veto.utils.Notifications.CHANNEL_FAILED,
                com.neubofy.veto.ui.settings.LogViewActivity.class
            );
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MainPageFragment())
                    .commit();
            handleIntent(getIntent());
        }

        // Silently check for OTA updates
        UpdateManager.INSTANCE.checkForUpdates(this, true, false, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TempContactExpiredService.scheduleJob(this, 0);
    }
}
