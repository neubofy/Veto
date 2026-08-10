package com.neubofy.veto.ui.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.neubofy.veto.R
import com.neubofy.veto.data.EncryptedSettingsRepository
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.UiUtil
import com.neubofy.veto.ui.VetoActivity
import com.neubofy.veto.ui.common.PasswordSetDialog
import com.neubofy.veto.utils.CypherUtils
import java.util.Locale

class SettingsActivity : VetoActivity(), CompoundButton.OnCheckedChangeListener {

    private lateinit var settings: SettingsRepository
    private lateinit var encSettings: EncryptedSettingsRepository

    private lateinit var switchDeviceWipe: MaterialSwitch
    private lateinit var switchVetoviaPin: MaterialSwitch
    private lateinit var switchTheftAutoDetect: MaterialSwitch

    private lateinit var textStatusWipe: TextView
    private lateinit var textStatusPin: TextView
    private lateinit var textStatusLockMsg: TextView
    private lateinit var textStatusTheftContact: TextView
    private lateinit var textStatusCommand: TextView
    private lateinit var textSelectedRingtone: TextView

    private lateinit var btnEditWipe: Button
    private lateinit var btnRemoveWipe: Button
    private lateinit var btnEditPin: Button
    private lateinit var btnRemovePin: Button
    private lateinit var btnEditLockMsg: Button
    private lateinit var btnRemoveLockMsg: Button
    private lateinit var btnEditTheftContact: Button
    private lateinit var btnRemoveTheftContact: Button
    private lateinit var btnEditCommand: Button

    private lateinit var buttonSelectRingtone: Button
    private lateinit var btnViewLogs: Button
    private lateinit var btnAboutVeto: Button

    companion object {
        private const val REQUEST_CODE_RINGTONE = 5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        UiUtil.setupEdgeToEdgeAppBar(findViewById(R.id.appBar))
        UiUtil.setupEdgeToEdgeScrollView(findViewById(R.id.scrollView))

        settings = SettingsRepository.getInstance(this)
        encSettings = EncryptedSettingsRepository.getInstance(this)

        switchDeviceWipe = findViewById(R.id.switchDeviceWipe)
        switchDeviceWipe.isChecked = settings.get(Settings.SET_WIPE_ENABLED) as Boolean
        switchDeviceWipe.setOnCheckedChangeListener(this)

        switchVetoviaPin = findViewById(R.id.switchVetoviaPin)
        switchVetoviaPin.isChecked = settings.get(Settings.SET_ACCESS_VIA_PIN) as Boolean
        switchVetoviaPin.setOnCheckedChangeListener(this)

        switchTheftAutoDetect = findViewById(R.id.switchTheftAutoDetect)
        switchTheftAutoDetect.isChecked = settings.get(Settings.SET_THEFT_AUTO_DETECT_ENABLED) as Boolean
        switchTheftAutoDetect.setOnCheckedChangeListener(this)

        textStatusWipe = findViewById(R.id.textStatusWipe)
        textStatusPin = findViewById(R.id.textStatusPin)
        textStatusLockMsg = findViewById(R.id.textStatusLockMsg)
        textStatusTheftContact = findViewById(R.id.textStatusTheftContact)
        textStatusCommand = findViewById(R.id.textStatusCommand)
        textSelectedRingtone = findViewById(R.id.textSelectedRingtone)

        btnEditWipe = findViewById(R.id.btnEditWipe)
        btnRemoveWipe = findViewById(R.id.btnRemoveWipe)
        btnEditPin = findViewById(R.id.btnEditPin)
        btnRemovePin = findViewById(R.id.btnRemovePin)
        btnEditLockMsg = findViewById(R.id.btnEditLockMsg)
        btnRemoveLockMsg = findViewById(R.id.btnRemoveLockMsg)
        btnEditTheftContact = findViewById(R.id.btnEditTheftContact)
        btnRemoveTheftContact = findViewById(R.id.btnRemoveTheftContact)
        btnEditCommand = findViewById(R.id.btnEditCommand)

        setupInfoButton(R.id.btnInfoWipe, "Remote Wipe", getString(R.string.delete_pw_warning_no_backup) + "\n\n" + getString(R.string.Settings_VetoCommand_Description))
        setupInfoButton(R.id.btnInfoPin, "Veto PIN", getString(R.string.Settings_Veto_via_Pin_Description))
        setupInfoButton(R.id.btnInfoLockMsg, "Lock Screen Message", getString(R.string.Settings_Lockscreenmessage_Description))
        setupInfoButton(R.id.btnInfoTheftContact, "Theft Contact Info", "Enter an alternate phone number or email address to display on the screen when theft mode is active, so the device can be returned to you.")
        setupInfoButton(R.id.btnInfoTheftAutoDetect, "Auto Theft Detection", "If enabled, theft mode will automatically activate if someone removes your SIM card.")
        setupInfoButton(R.id.btnInfoCommand, "Trigger Command", getString(R.string.Settings_VetoCommand_Description))

        btnEditWipe.setOnClickListener { onEnterDeletePasswordClicked() }
        btnRemoveWipe.setOnClickListener {
            encSettings.setDeletePassword(null)
            updateUI()
        }

        btnEditPin.setOnClickListener { onEnterPinClicked() }
        btnRemovePin.setOnClickListener {
            encSettings.setVetoPin(null)
            updateUI()
        }

        btnEditLockMsg.setOnClickListener { onEditLockMsgClicked() }
        btnRemoveLockMsg.setOnClickListener {
            settings.set(Settings.SET_LOCKSCREEN_MESSAGE, "")
            updateUI()
        }

        btnEditTheftContact.setOnClickListener { onEditTheftContactClicked() }
        btnRemoveTheftContact.setOnClickListener {
            settings.set(Settings.SET_THEFT_CONTACT_INFO, "")
            updateUI()
        }

        btnEditCommand.setOnClickListener { onEditCommandClicked() }

        buttonSelectRingtone = findViewById(R.id.buttonSelectRingTone)
        buttonSelectRingtone.setOnClickListener { onSelectRingtoneClicked() }

        btnViewLogs = findViewById(R.id.btnViewLogs)
        btnViewLogs.setOnClickListener { startActivity(Intent(this, LogViewActivity::class.java)) }

        btnAboutVeto = findViewById(R.id.btnAboutVeto)
        btnAboutVeto.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }

        updateUI()

        val sliderVolumeInterval = findViewById<com.google.android.material.slider.Slider>(R.id.sliderVolumeInterval)
        val textVolumeIntervalValue = findViewById<TextView>(R.id.textVolumeIntervalValue)
        
        val currentInterval = settings.get(Settings.SET_VOLUME_ENFORCE_INTERVAL) as Int
        sliderVolumeInterval.value = currentInterval.toFloat()
        textVolumeIntervalValue.text = "${currentInterval}s"
        
        sliderVolumeInterval.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                val interval = value.toInt()
                settings.set(Settings.SET_VOLUME_ENFORCE_INTERVAL, interval)
                textVolumeIntervalValue.text = "${interval}s"
            }
        }
    }

    private fun setupInfoButton(id: Int, title: String, message: String) {
        val btn = findViewById<ImageButton>(id)
        btn?.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun updateUI() {
        val wipePw = encSettings.getDeletePassword()
        if (!wipePw.isNullOrBlank()) {
            textStatusWipe.text = "Password set"
            btnEditWipe.text = "Change"
            btnRemoveWipe.visibility = View.VISIBLE
        } else {
            textStatusWipe.text = "Not set"
            btnEditWipe.text = "Set Password"
            btnRemoveWipe.visibility = View.GONE
        }

        val pin = encSettings.getVetoPin()
        if (!pin.isNullOrBlank()) {
            textStatusPin.text = "PIN set"
            btnEditPin.text = "Change"
            btnRemovePin.visibility = View.VISIBLE
        } else {
            textStatusPin.text = "Not set"
            btnEditPin.text = "Set PIN"
            btnRemovePin.visibility = View.GONE
        }

        val lockMsg = settings.get(Settings.SET_LOCKSCREEN_MESSAGE) as String
        if (lockMsg.isNotEmpty()) {
            textStatusLockMsg.text = "\"$lockMsg\""
            btnEditLockMsg.text = "Change"
            btnRemoveLockMsg.visibility = View.VISIBLE
        } else {
            textStatusLockMsg.text = "Not set"
            btnEditLockMsg.text = "Set Message"
            btnRemoveLockMsg.visibility = View.GONE
        }

        val theftContact = settings.get(Settings.SET_THEFT_CONTACT_INFO) as String
        if (theftContact.isNotEmpty()) {
            textStatusTheftContact.text = "\"$theftContact\""
            btnEditTheftContact.text = "Change"
            btnRemoveTheftContact.visibility = View.VISIBLE
        } else {
            textStatusTheftContact.text = "Not set"
            btnEditTheftContact.text = "Set Contact Info"
            btnRemoveTheftContact.visibility = View.GONE
        }

        val cmd = settings.get(Settings.SET_Veto_COMMAND) as String
        textStatusCommand.text = cmd

        val ringtoneUriStr = settings.get(Settings.SET_RINGER_TONE) as String
        val ringtone = RingtoneManager.getRingtone(this, Uri.parse(ringtoneUriStr))
        if (ringtone != null) {
            textSelectedRingtone.text = ringtone.getTitle(this)
        } else {
            textSelectedRingtone.text = "Default"
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (buttonView.id) {
            R.id.switchDeviceWipe -> settings.set(Settings.SET_WIPE_ENABLED, isChecked)
            R.id.switchVetoviaPin -> settings.set(Settings.SET_ACCESS_VIA_PIN, isChecked)
            R.id.switchTheftAutoDetect -> settings.set(Settings.SET_THEFT_AUTO_DETECT_ENABLED, isChecked)
        }
    }

    private fun onEnterDeletePasswordClicked() {
        PasswordSetDialog.showPasswordSetDialog(
            context = this,
            title = "Set Remote Delete Password",
            positiveButtonText = "Set Password",
            message = "Warning: Please remember this password. It is required to execute the remote wipe command.",
            minLength = CypherUtils.MIN_PASSWORD_LENGTH,
            onSuccess = { password ->
                encSettings.setDeletePassword(password)
                updateUI()
            }
        )
    }

    private fun onEnterPinClicked() {
        PasswordSetDialog.showPasswordSetDialog(
            context = this,
            title = "Set Veto PIN",
            positiveButtonText = "Set PIN",
            message = "Set a PIN to authorize commands sent via notification reply or unlisted contacts.",
            minLength = 1,
            onSuccess = { pin ->
                encSettings.setVetoPin(pin)
                updateUI()
            }
        )
    }

    private fun onEditLockMsgClicked() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(settings.get(Settings.SET_LOCKSCREEN_MESSAGE) as String)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Lock Screen Message")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val edited = input.text.toString()
                settings.set(Settings.SET_LOCKSCREEN_MESSAGE, edited)
                updateUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onEditTheftContactClicked() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(settings.get(Settings.SET_THEFT_CONTACT_INFO) as String)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Theft Contact Info")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val edited = input.text.toString()
                settings.set(Settings.SET_THEFT_CONTACT_INFO, edited)
                updateUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onEditCommandClicked() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(settings.get(Settings.SET_Veto_COMMAND) as String)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Trigger Command Word")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val edited = input.text.toString()
                if (edited.isEmpty()) {
                    Toast.makeText(this, getString(R.string.Toast_Empty_VetoCommand), Toast.LENGTH_LONG).show()
                    settings.set(Settings.SET_Veto_COMMAND, "veto")
                } else {
                    settings.set(Settings.SET_Veto_COMMAND, edited.lowercase(Locale.ROOT))
                }
                updateUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onSelectRingtoneClicked() {
        val currentUri = Uri.parse(settings.get(Settings.SET_RINGER_TONE) as String)
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_CODE_RINGTONE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_RINGTONE && resultCode == RESULT_OK && data != null) {
            @Suppress("DEPRECATION")
            val uri = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                settings.set(Settings.SET_RINGER_TONE, uri.toString())
                updateUI()
            }
        }
    }
}
