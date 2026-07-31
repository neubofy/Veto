package com.neubofy.veto.ui.onboarding;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.neubofy.veto.R;
import com.neubofy.veto.data.Settings;
import com.neubofy.veto.data.SettingsRepository;
// Removed server imports
import com.neubofy.veto.ui.MainActivity;
import com.neubofy.veto.utils.Notifications;
import com.neubofy.veto.utils.UnregisterUtil;

public class UpdateboardingModernCryptoActivity extends AppCompatActivity {

    private static final int EXPORT_REQ_CODE = 30;

    private SettingsRepository settings;

    boolean isRegisteredWithServer;
    boolean isPinSet;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_updateboarding_modern_crypto);

        settings = SettingsRepository.Companion.getInstance(this);
        isRegisteredWithServer = settings.serverAccountExists();
        isPinSet = !settings.get(Settings.SET_PIN).equals("");

        if (!isRegisteredWithServer && !isPinSet) {
            completeAndContinueToMain();
        } else {
            if (!isPinSet) {
                findViewById(R.id.sectionVetoPin).setVisibility(View.GONE);
            }
            if (!isRegisteredWithServer) {
                findViewById(R.id.sectionVetoServer).setVisibility(View.GONE);
            }
        }
        findViewById(R.id.buttonExport).setOnClickListener(this::onExportSettingsClicked);
        findViewById(R.id.buttonExit).setOnClickListener(this::onExitClicked);
        findViewById(R.id.buttonConfirm).setOnClickListener(this::onConfirmClicked);
    }

    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EXPORT_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    new SettingsImportExporter(this).exportData(uri);
                }
            }
        }
    }
    */

    private void onExportSettingsClicked(View view) {
        Toast.makeText(view.getContext(), "Sorry, no longer supported.", Toast.LENGTH_LONG).show();

//        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
//        intent.putExtra(Intent.EXTRA_TITLE, SETTINGS_FILENAME);
//        intent.setType("*/*");
//        startActivityForResult(intent, EXPORT_REQ_CODE);
    }

    private void onExitClicked(View view) {
        // Don't set the UPDATEBOARDING_COMPLETED flag
        finish();
    }

    private void onConfirmClicked(View view) {
        if (isPinSet) {
            settings.set(Settings.SET_PIN, "");
        }
            // We removed the Veto server API entirely. Just continue.
            completeAndContinueToMain();
    }

    private void completeAndContinueToMain() {
        settings.set(Settings.SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED, true);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public static void notifyAboutCryptoRefreshIfRequired(Context context) {
        SettingsRepository settings = SettingsRepository.Companion.getInstance(context);
        boolean alreadyCompleted = (Boolean) settings.get(Settings.SET_UPDATEBOARDING_MODERN_CRYPTO_COMPLETED);
        if (alreadyCompleted) return;

        boolean isRegisteredWithServer = settings.serverAccountExists();
        boolean isPinSet = !settings.get(Settings.SET_PIN).equals("");

        if (isRegisteredWithServer || isPinSet) {
            String title = context.getString(R.string.notify_crypto_update_title);
            String text = context.getString(R.string.notify_crypto_update_text);
            Notifications.notify(context, title, text, Notifications.CHANNEL_SECURITY);
        }
    }
}
