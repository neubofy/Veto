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
import com.neubofy.veto.ui.onboarding.UpdateboardingModernCryptoActivity;

import com.neubofy.veto.ui.settings.AboutActivity;
import com.neubofy.veto.utils.UpdateManager;
import kotlin.Unit;

public class MainActivity extends VetoActivity {

    SettingsRepository settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
        setSupportActionBar(toolbar);

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));

        settings = SettingsRepository.Companion.getInstance(this);
        settings.load();

        if (((Integer) settings.get(Settings.SET_APP_CRASHED_LOG_ENTRY)) == 1) {
            startActivity(new Intent(this, CrashedActivity.class));
            finish();
            return;
        }

        if (!(Boolean) settings.get(Settings.SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED)) {
            startActivity(new Intent(this, UpdateboardingModernCryptoActivity.class));
            finish();
            return;
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MainPageFragment())
                    .commit();
        }

        // Silently check for OTA updates
        UpdateManager.INSTANCE.checkForUpdates(this, true, false, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TempContactExpiredService.scheduleJob(this, 0);
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_log) {
            startActivity(new Intent(this, com.neubofy.veto.ui.settings.LogViewActivity.class));
            return true;
        } else if (id == R.id.action_about) {
            startActivity(new Intent(this, com.neubofy.veto.ui.settings.AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
