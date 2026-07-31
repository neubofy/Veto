package com.neubofy.veto.ui.settings;

import static com.neubofy.veto.commands.CommandHandlerKt.availableCommands;
import static com.neubofy.veto.ui.UiUtil.setupEdgeToEdgeAppBar;
import static com.neubofy.veto.ui.UiUtil.setupEdgeToEdgeScrollView;
import static com.neubofy.veto.utils.CypherUtils.MIN_PASSWORD_LENGTH;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.neubofy.veto.R;
import com.neubofy.veto.data.EncryptedSettingsRepository;
import com.neubofy.veto.data.Settings;
import com.neubofy.veto.data.SettingsRepository;
import com.neubofy.veto.ui.VetoActivity;
import com.neubofy.veto.ui.common.PasswordSetDialog;
import kotlin.Unit;

public class SettingsActivity extends VetoActivity implements CompoundButton.OnCheckedChangeListener {

    private SettingsRepository settings;
    private EncryptedSettingsRepository encSettings;

    private CheckBox checkBoxDeviceWipe;
    private CheckBox checkBoxAccessViaPin;
    
    // Status text views
    private TextView textStatusWipe;
    private TextView textStatusPin;
    private TextView textStatusLockMsg;
    private TextView textStatusCommand;
    private TextView textSelectedRingtone;

    // Action buttons
    private Button btnEditWipe, btnRemoveWipe;
    private Button btnEditPin, btnRemovePin;
    private Button btnEditLockMsg, btnRemoveLockMsg;
    private Button btnEditCommand;
    
    private Button buttonSelectRingtone;
    private com.google.android.material.materialswitch.MaterialSwitch switchAutoUpload;
    private Button buttonManualLocate;

    private static final int REQUEST_CODE_RINGTONE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar));
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView));

        settings = SettingsRepository.Companion.getInstance(this);
        encSettings = EncryptedSettingsRepository.Companion.getInstance(this);

        // Security Section
        checkBoxDeviceWipe = findViewById(R.id.checkBoxWipeData);
        checkBoxDeviceWipe.setChecked((Boolean) settings.get(Settings.SET_WIPE_ENABLED));
        checkBoxDeviceWipe.setOnCheckedChangeListener(this);
        
        checkBoxAccessViaPin = findViewById(R.id.checkBoxVetoviaPin);
        checkBoxAccessViaPin.setChecked((Boolean) settings.get(Settings.SET_ACCESS_VIA_PIN));
        checkBoxAccessViaPin.setOnCheckedChangeListener(this);
        
        // Status Views
        textStatusWipe = findViewById(R.id.textStatusWipe);
        textStatusPin = findViewById(R.id.textStatusPin);
        textStatusLockMsg = findViewById(R.id.textStatusLockMsg);
        textStatusCommand = findViewById(R.id.textStatusCommand);
        textSelectedRingtone = findViewById(R.id.textSelectedRingtone);
        
        // Action Buttons
        btnEditWipe = findViewById(R.id.btnEditWipe);
        btnRemoveWipe = findViewById(R.id.btnRemoveWipe);
        btnEditPin = findViewById(R.id.btnEditPin);
        btnRemovePin = findViewById(R.id.btnRemovePin);
        btnEditLockMsg = findViewById(R.id.btnEditLockMsg);
        btnRemoveLockMsg = findViewById(R.id.btnRemoveLockMsg);
        btnEditCommand = findViewById(R.id.btnEditCommand);

        // Info Buttons
        setupInfoButton(R.id.btnInfoWipe, "Remote Wipe", getString(R.string.delete_pw_warning_no_backup) + "\n\n" + getString(R.string.Settings_LCLDCommand_Description));
        setupInfoButton(R.id.btnInfoPin, "Veto PIN", getString(R.string.Settings_LCLD_via_Pin_Description));
        setupInfoButton(R.id.btnInfoLockMsg, "Lock Screen Message", getString(R.string.Settings_Lockscreenmessage_Description));
        setupInfoButton(R.id.btnInfoCommand, "Trigger Command", getString(R.string.Settings_LCLDCommand_Description));

        // Click Listeners
        btnEditWipe.setOnClickListener(this::onEnterDeletePasswordClicked);
        btnRemoveWipe.setOnClickListener(v -> { encSettings.setDeletePassword(null); updateUI(); });
        
        btnEditPin.setOnClickListener(this::onEnterPinClicked);
        btnRemovePin.setOnClickListener(v -> { encSettings.setVetoPin(null); updateUI(); });
        
        btnEditLockMsg.setOnClickListener(this::onEditLockMsgClicked);
        btnRemoveLockMsg.setOnClickListener(v -> { settings.set(Settings.SET_LOCKSCREEN_MESSAGE, ""); updateUI(); });
        
        btnEditCommand.setOnClickListener(this::onEditCommandClicked);

        // Device Control
        buttonSelectRingtone = findViewById(R.id.buttonSelectRingTone);
        buttonSelectRingtone.setOnClickListener(this::onSelectRingtoneClicked);

        // Background Tracking
        switchAutoUpload = findViewById(R.id.switchAutoUpload);
        switchAutoUpload.setChecked(isAutoLocActive());
        switchAutoUpload.setOnCheckedChangeListener(this::onAutoUploadCheckedChanged);

        buttonManualLocate = findViewById(R.id.buttonManualLocate);
        buttonManualLocate.setOnClickListener(v -> manualUpdateLocation());
        
        updateUI();
    }
    
    private void setupInfoButton(int id, String title, String message) {
        ImageButton btn = findViewById(id);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(SettingsActivity.this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
            });
        }
    }
    
    private void updateUI() {
        // Wipe Password
        String wipePw = encSettings.getDeletePassword();
        if (wipePw == null || wipePw.isBlank()) {
            textStatusWipe.setText("🔴 Password Not Set");
            textStatusWipe.setTextColor(getColor(R.color.md_theme_error));
            btnEditWipe.setText("Set");
            btnRemoveWipe.setVisibility(View.GONE);
        } else {
            textStatusWipe.setText("🟢 Password Set");
            textStatusWipe.setTextColor(getColor(R.color.colorPrimary));
            btnEditWipe.setText("Edit");
            btnRemoveWipe.setVisibility(View.VISIBLE);
        }
        
        // Veto PIN
        String pin = encSettings.getVetoPin();
        if (pin == null || pin.isBlank()) {
            textStatusPin.setText("🔴 PIN Not Set");
            textStatusPin.setTextColor(getColor(R.color.md_theme_error));
            btnEditPin.setText("Set");
            btnRemovePin.setVisibility(View.GONE);
        } else {
            textStatusPin.setText("🟢 PIN Set");
            textStatusPin.setTextColor(getColor(R.color.colorPrimary));
            btnEditPin.setText("Edit");
            btnRemovePin.setVisibility(View.VISIBLE);
        }
        
        // Lock Screen Message
        String lockMsg = (String) settings.get(Settings.SET_LOCKSCREEN_MESSAGE);
        if (lockMsg == null || lockMsg.isBlank()) {
            textStatusLockMsg.setText("🔴 Not Set");
            textStatusLockMsg.setTextColor(getColor(R.color.md_theme_error));
            btnEditLockMsg.setText("Set");
            btnRemoveLockMsg.setVisibility(View.GONE);
        } else {
            textStatusLockMsg.setText("🟢 " + lockMsg);
            textStatusLockMsg.setTextColor(getColor(R.color.colorPrimary));
            btnEditLockMsg.setText("Edit");
            btnRemoveLockMsg.setVisibility(View.VISIBLE);
        }
        
        // Command
        String cmd = (String) settings.get(Settings.SET_Veto_COMMAND);
        if (cmd == null || cmd.isBlank()) {
            cmd = "veto";
        }
        textStatusCommand.setText("🟢 " + cmd);
        textStatusCommand.setTextColor(getColor(R.color.colorPrimary));
        
        // Ringtone
        String ringtoneUriString = (String) settings.get(Settings.SET_RINGER_TONE);
        if (ringtoneUriString != null && !ringtoneUriString.isEmpty()) {
            Uri ringtoneUri = Uri.parse(ringtoneUriString);
            Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
            if (ringtone != null) {
                textSelectedRingtone.setText(ringtone.getTitle(this));
            } else {
                textSelectedRingtone.setText("Unknown");
            }
        } else {
            textSelectedRingtone.setText("Default");
        }
    }
    
    private void onEditLockMsgClicked(View v) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText((String) settings.get(Settings.SET_LOCKSCREEN_MESSAGE));
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Set Lock Screen Message")
            .setView(input)
            .setPositiveButton("Save", (dialog, which) -> {
                settings.set(Settings.SET_LOCKSCREEN_MESSAGE, input.getText().toString());
                updateUI();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void onEditCommandClicked(View v) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText((String) settings.get(Settings.SET_Veto_COMMAND));
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("Set Trigger Command Word")
            .setView(input)
            .setPositiveButton("Save", (dialog, which) -> {
                String edited = input.getText().toString();
                if (edited.isEmpty()) {
                    Toast.makeText(this, getString(R.string.Toast_Empty_LCLDCommand), Toast.LENGTH_LONG).show();
                    settings.set(Settings.SET_Veto_COMMAND, "veto");
                } else {
                    settings.set(Settings.SET_Veto_COMMAND, edited.toLowerCase());
                }
                updateUI();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void onAutoUploadCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            CharSequence[] options = new CharSequence[]{"15 Minutes", "30 Minutes", "1 Hour", "2 Hours", "6 Hours"};
            int[] values = new int[]{15, 30, 60, 120, 360};
            
            int currentVal = (int) settings.get(Settings.SET_VetoSERVER_UPDATE_TIME);
            int defaultSelection = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == currentVal) {
                    defaultSelection = i;
                    break;
                }
            }

            new com.google.android.material.dialog.MaterialAlertDialogBuilder(SettingsActivity.this)
                .setTitle("Select Update Interval")
                .setCancelable(false)
                .setSingleChoiceItems(options, defaultSelection, (dialog, which) -> {
                    settings.set(Settings.SET_VetoSERVER_UPDATE_TIME, values[which]);
                    startAutoLoc();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    switchAutoUpload.setOnCheckedChangeListener(null);
                    switchAutoUpload.setChecked(false);
                    switchAutoUpload.setOnCheckedChangeListener(this::onAutoUploadCheckedChanged);
                })
                .show();
        } else {
            stopAutoLoc();
        }
    }

    private boolean isAutoLocActive() {
        try {
            return androidx.work.WorkManager.getInstance(this)
                .getWorkInfosForUniqueWork(com.neubofy.veto.commands.AutoLocCommand.WORK_NAME)
                .get()
                .stream()
                .anyMatch(info -> info.getState() == androidx.work.WorkInfo.State.ENQUEUED || info.getState() == androidx.work.WorkInfo.State.RUNNING);
        } catch (Exception e) {
            return false;
        }
    }

    private void startAutoLoc() {
        try {
            int intervalMinutes = (int) settings.get(Settings.SET_VetoSERVER_UPDATE_TIME);
            String locateCommand = settings.get(Settings.SET_Veto_COMMAND).toString() + " locate gps";

            androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putString(com.neubofy.veto.workers.CommandExecutionWorker.KEY_COMMAND, locateCommand)
                .putString(com.neubofy.veto.workers.CommandExecutionWorker.KEY_TRANSPORT_TYPE, com.neubofy.veto.workers.CommandExecutionWorker.TRANS_Veto_SERVER)
                .putString(com.neubofy.veto.workers.CommandExecutionWorker.KEY_DESTINATION, "Background_Upload")
                .build();

            androidx.work.PeriodicWorkRequest periodicWork = new androidx.work.PeriodicWorkRequest.Builder(
                com.neubofy.veto.workers.CommandExecutionWorker.class,
                intervalMinutes,
                java.util.concurrent.TimeUnit.MINUTES
            ).setInputData(inputData).build();

            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                com.neubofy.veto.commands.AutoLocCommand.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                periodicWork
            );
            Toast.makeText(this, "Background Location Upload Started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error starting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            switchAutoUpload.setChecked(false);
        }
    }

    private void stopAutoLoc() {
        androidx.work.WorkManager.getInstance(this).cancelUniqueWork(com.neubofy.veto.commands.AutoLocCommand.WORK_NAME);
        Toast.makeText(this, "Background Location Upload Stopped", Toast.LENGTH_SHORT).show();
    }

    private void manualUpdateLocation() {
        Toast.makeText(this, "Locating...", Toast.LENGTH_SHORT).show();
        SettingsRepository settings = SettingsRepository.Companion.getInstance(this);
        String locateCommand = (String) settings.get(com.neubofy.veto.data.Settings.SET_Veto_COMMAND) + " locate gps";
        
        androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putString(com.neubofy.veto.workers.CommandExecutionWorker.KEY_COMMAND, locateCommand)
                .putString(com.neubofy.veto.workers.CommandExecutionWorker.KEY_TRANSPORT_TYPE, com.neubofy.veto.workers.CommandExecutionWorker.TRANS_Veto_SERVER)
                .putString(com.neubofy.veto.workers.CommandExecutionWorker.KEY_DESTINATION, "Manual_Upload")
                .build();

        androidx.work.WorkRequest workRequest = new androidx.work.OneTimeWorkRequest.Builder(com.neubofy.veto.workers.CommandExecutionWorker.class)
                .setInputData(inputData)
                .build();

        androidx.work.WorkManager.getInstance(this).enqueue(workRequest);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == checkBoxDeviceWipe) {
            settings.set(Settings.SET_WIPE_ENABLED, isChecked);
        } else if (buttonView == checkBoxAccessViaPin) {
            settings.set(Settings.SET_ACCESS_VIA_PIN, isChecked);
        }
    }

    private void onEnterPinClicked(View v) {
        Context context = v.getContext();
        View pinLayout = getLayoutInflater().inflate(R.layout.dialog_pin, null);

        EditText editTextPin = pinLayout.findViewById(R.id.editTextPin);
        
        // Show current pin if set
        String currentPin = encSettings.getVetoPin();
        if (currentPin != null) {
            editTextPin.setText(currentPin);
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.Settings_Enter_Pin))
                .setView(pinLayout)
                .setPositiveButton(getString(R.string.Ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String pin = editTextPin.getText().toString();

                        if (pin.isBlank()) {
                            encSettings.setVetoPin(null);
                        } else if (availableCommands(context).stream().anyMatch(cmd -> cmd.getKeyword().equals(pin))) {
                            Toast.makeText(context, R.string.pin_match_command_keyword, Toast.LENGTH_LONG).show();
                        } else if (pin.length() < MIN_PASSWORD_LENGTH) {
                            Toast.makeText(context, R.string.pin_min_length, Toast.LENGTH_LONG).show();
                        } else {
                            encSettings.setVetoPin(pin);
                        }
                        updateUI();
                    }
                })
                .show();
    }

    private void onEnterDeletePasswordClicked(View v) {
        new PasswordSetDialog(v.getContext(), (newPassword) -> {
            encSettings.setDeletePassword(newPassword);
            updateUI();
            return Unit.INSTANCE;
        }).show();
    }

    private void onSelectRingtoneClicked(View v) {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.Settings_Select_Ringtone));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse((String) settings.get(Settings.SET_RINGER_TONE)));
        try {
            this.startActivityForResult(intent, REQUEST_CODE_RINGTONE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, getString(R.string.Settings_no_ringtone_picker), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_RINGTONE) {
            Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                settings.set(Settings.SET_RINGER_TONE, uri.toString());
            } else {
                settings.set(Settings.SET_RINGER_TONE, "");
            }
            updateUI();
        }
    }

}
