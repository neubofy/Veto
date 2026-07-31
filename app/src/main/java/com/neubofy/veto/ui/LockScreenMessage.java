package com.neubofy.veto.ui;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;

import com.neubofy.veto.R;
import com.neubofy.veto.data.SettingsRepository;
import com.neubofy.veto.utils.SingletonHolder;

public class LockScreenMessage extends VetoActivity {

    public static final String CUSTOM_TEXT = "CUSTOM_TEXT";
    private SettingsRepository settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ensure it shows over lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_lock_screen_message);

        settings = SettingsRepository.Companion.getInstance(this);
        settings.load();
        
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing, block back button
            }
        });

        TextView textView = findViewById(R.id.textViewLockScreenMessage);
        String message = getIntent().getStringExtra(CUSTOM_TEXT);
        if (message != null && !message.isEmpty()) {
            textView.setText(message);
        }

        Button buttonUnlock = findViewById(R.id.buttonUnlock);

        buttonUnlock.setOnClickListener(v -> {
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                    @Override
                    public void onDismissSucceeded() {
                        super.onDismissSucceeded();
                        finish();
                    }
                    @Override
                    public void onDismissCancelled() {
                        super.onDismissCancelled();
                        // User cancelled the pin pad, the activity remains on screen!
                    }
                    @Override
                    public void onDismissError() {
                        super.onDismissError();
                        // Fallback just in case
                        finish();
                    }
                });
            } else {
                finish();
            }
        });
        
        hideSystemUI();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Prevent back and other buttons from closing the activity
        return true;
    }

    @Override
    public void onBackPressed() {
        // Do nothing
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }
}
