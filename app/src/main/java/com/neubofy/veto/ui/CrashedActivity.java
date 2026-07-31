package com.neubofy.veto.ui;

import static com.neubofy.veto.ui.UiUtil.setupEdgeToEdgeAppBar;
import static com.neubofy.veto.ui.UiUtil.setupEdgeToEdgeScrollView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.neubofy.veto.R;
import com.neubofy.veto.data.LogEntry;
import com.neubofy.veto.data.LogRepository;
import com.neubofy.veto.data.Settings;
import com.neubofy.veto.data.SettingsRepository;
import com.neubofy.veto.utils.Utils;

public class CrashedActivity extends VetoActivity {

    private String crashLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crashed);

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView));

        SettingsRepository settings = SettingsRepository.Companion.getInstance(this);
        settings.set(Settings.SET_APP_CRASHED_LOG_ENTRY, 0);

        LogRepository repo = LogRepository.Companion.getInstance(this);
        LogEntry entry = repo.getLastCrashLog();
        if (entry == null) {
            continueToMain();
            return;
        }
        crashLog = entry.getMsg();

        TextView textViewCrashLog = findViewById(R.id.textViewCrash);
        textViewCrashLog.setText(crashLog);

        Button buttonSendLog = findViewById(R.id.buttonSendLog);
        buttonSendLog.setOnClickListener(this::onSendLogClicked);

        Button buttonCopy = findViewById(R.id.buttonCopyLog);
        buttonCopy.setOnClickListener(this::onCopyClicked);

        Button buttonContinue = findViewById(R.id.buttonContinue);
        buttonContinue.setOnClickListener(this::onContinueClicked);
    }

    private void onSendLogClicked(View v) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://github.com/neubofy/Veto/issues"));
        startActivity(intent);
        finish();
    }

    private void onCopyClicked(View v) {
        Utils.copyToClipboard(v.getContext(), "CrashLog", crashLog);
    }

    private void onContinueClicked(View v) {
        continueToMain();
    }

    private void continueToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
