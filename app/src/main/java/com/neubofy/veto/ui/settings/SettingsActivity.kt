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
import com.google.firebase.auth.FirebaseAuth
import com.neubofy.veto.ui.common.PasswordSetDialog
import com.neubofy.veto.utils.CypherUtils
import java.util.Locale
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject

class SettingsActivity : VetoActivity(), CompoundButton.OnCheckedChangeListener {

    private lateinit var settings: SettingsRepository
    private lateinit var encSettings: EncryptedSettingsRepository

    private lateinit var switchVetoviaPin: MaterialSwitch
    private lateinit var switchTheftAutoDetect: MaterialSwitch
    private lateinit var switchTheftWrongPass: MaterialSwitch

    private lateinit var textStatusPin: TextView
    private lateinit var textStatusLockMsg: TextView
    private lateinit var textStatusTheftContact: TextView
    private lateinit var textStatusCommand: TextView
    private lateinit var textSelectedRingtone: TextView
    private lateinit var textStatusWrongPass: TextView

    private lateinit var btnEditPin: Button
    private lateinit var btnRemovePin: Button
    private lateinit var btnEditLockMsg: Button
    private lateinit var btnRemoveLockMsg: Button
    private lateinit var btnEditTheftContact: Button
    private lateinit var btnRemoveTheftContact: Button
    private lateinit var btnEditCommand: Button
    private lateinit var btnEditWrongPass: Button

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

        switchVetoviaPin = findViewById(R.id.switchVetoviaPin)
        switchVetoviaPin.isChecked = settings.get(Settings.SET_ACCESS_VIA_PIN) as Boolean
        switchVetoviaPin.setOnCheckedChangeListener(this)

        switchTheftAutoDetect = findViewById(R.id.switchTheftAutoDetect)
        switchTheftAutoDetect.isChecked = settings.get(Settings.SET_THEFT_AUTO_DETECT_ENABLED) as Boolean
        switchTheftAutoDetect.setOnCheckedChangeListener(this)
        
        switchTheftWrongPass = findViewById(R.id.switchTheftWrongPass)
        switchTheftWrongPass.isChecked = settings.get(Settings.SET_THEFT_WRONG_PASS_ENABLED) as Boolean
        switchTheftWrongPass.setOnCheckedChangeListener(this)

        textStatusPin = findViewById(R.id.textStatusPin)
        textStatusLockMsg = findViewById(R.id.textStatusLockMsg)
        textStatusTheftContact = findViewById(R.id.textStatusTheftContact)
        textStatusCommand = findViewById(R.id.textStatusCommand)
        textSelectedRingtone = findViewById(R.id.textSelectedRingtone)
        textStatusWrongPass = findViewById(R.id.textStatusWrongPass)

        btnEditPin = findViewById(R.id.btnEditPin)
        btnRemovePin = findViewById(R.id.btnRemovePin)
        btnEditLockMsg = findViewById(R.id.btnEditLockMsg)
        btnRemoveLockMsg = findViewById(R.id.btnRemoveLockMsg)
        btnEditTheftContact = findViewById(R.id.btnEditTheftContact)
        btnRemoveTheftContact = findViewById(R.id.btnRemoveTheftContact)
        btnEditCommand = findViewById(R.id.btnEditCommand)
        btnEditWrongPass = findViewById(R.id.btnEditWrongPass)

        setupInfoButton(R.id.btnInfoPin, "Veto PIN", getString(R.string.Settings_Veto_via_Pin_Description))
        setupInfoButton(R.id.btnInfoLockMsg, "Lock Screen Message", getString(R.string.Settings_Lockscreenmessage_Description))
        setupInfoButton(R.id.btnInfoTheftContact, "Theft Contact Info", "Enter an alternate phone number or email address to display on the screen when theft mode is active, so the device can be returned to you.")
        setupInfoButton(R.id.btnInfoTheftAutoDetect, "Auto Theft Detection", "If enabled, theft mode will automatically activate if someone removes your SIM card.")
        setupInfoButton(R.id.btnInfoCommand, "Trigger Command", getString(R.string.Settings_VetoCommand_Description))
        setupInfoButton(R.id.btnInfoWrongPass, "Wrong Password Detection", "If enabled, entering the wrong lock screen password multiple times will trigger theft mode.")

        btnEditPin.setOnClickListener { handlePinChangeRequest { onEnterPinClicked() } }
        btnRemovePin.setOnClickListener {
            handlePinChangeRequest {
                encSettings.setVetoPin(null)
                updateUI()
            }
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
        btnEditWrongPass.setOnClickListener { onEditWrongPassClicked() }

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

        val sliderTheftSuspectedDuration = findViewById<com.google.android.material.slider.Slider>(R.id.sliderTheftSuspectedDuration)
        val textTheftSuspectedDurationValue = findViewById<TextView>(R.id.textTheftSuspectedDurationValue)
        
        val currentDuration = settings.get(Settings.SET_THEFT_SUSPECTED_DURATION) as Int
        sliderTheftSuspectedDuration.value = currentDuration.toFloat()
        textTheftSuspectedDurationValue.text = "${currentDuration}m"
        
        sliderTheftSuspectedDuration.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                val duration = value.toInt()
                settings.set(Settings.SET_THEFT_SUSPECTED_DURATION, duration)
                textTheftSuspectedDurationValue.text = "${duration}m"
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
        
        val wrongPassAttempts = settings.get(Settings.SET_THEFT_WRONG_PASS_ATTEMPTS) as Int
        textStatusWrongPass.text = "$wrongPassAttempts attempts allowed"
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (buttonView.id) {
            R.id.switchVetoviaPin -> settings.set(Settings.SET_ACCESS_VIA_PIN, isChecked)
            R.id.switchTheftAutoDetect -> settings.set(Settings.SET_THEFT_AUTO_DETECT_ENABLED, isChecked)
            R.id.switchTheftWrongPass -> settings.set(Settings.SET_THEFT_WRONG_PASS_ENABLED, isChecked)
        }
    }


    private fun handlePinChangeRequest(onProceed: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Warning: Cloud Data Deletion")
                .setMessage("Since you are logged into the Web Dashboard, changing or removing your PIN will permanently delete all your end-to-end encrypted cloud data (as it would become unreadable) and sign you out. Do you wish to proceed?")
                .setPositiveButton("Delete Data & Proceed") { _, _ ->
                    deleteCloudDataAndLogout(onProceed)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            onProceed()
        }
    }

    private fun deleteCloudDataAndLogout(onProceed: () -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        Toast.makeText(this, "Deleting Cloud Data...", Toast.LENGTH_SHORT).show()
        user.getIdToken(true).addOnCompleteListener { task ->
            if (task.isSuccessful && task.result?.token != null) {
                val token = task.result?.token!!
                val dashboardUrl = settings.get(Settings.SET_VetoSERVER_URL) as String

                Thread {
                    try {
                        val apiUrl = if (dashboardUrl.endsWith("/")) "${dashboardUrl}api/data/delete" else "$dashboardUrl/api/data/delete"
                        val url = URL(apiUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.setRequestProperty("Authorization", "Bearer $token")
                        connection.doOutput = true

                        val jsonParam = JSONObject()
                        jsonParam.put("token", token)

                        val out = java.io.OutputStreamWriter(connection.outputStream)
                        out.write(jsonParam.toString())
                        out.close()

                        val responseCode = connection.responseCode
                        runOnUiThread {
                            if (responseCode in 200..299) {
                                FirebaseAuth.getInstance().signOut()
                                settings.set(Settings.SET_SYNCED_FCM_TOKEN, "")
                                Toast.makeText(this@SettingsActivity, "Cloud Data Deleted and Signed Out", Toast.LENGTH_SHORT).show()
                                onProceed()
                            } else {
                                Toast.makeText(this@SettingsActivity, "Failed to delete cloud data. Server Error: $responseCode", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            } else {
                Toast.makeText(this, "Failed to authenticate for deletion.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onEnterPinClicked() {
        PasswordSetDialog.showPasswordSetDialog(
            context = this,
            title = "Set Veto PIN",
            positiveButtonText = "Set PIN",
            message = "Set a PIN to authorize commands sent via notification reply or unlisted contacts.",
            minLength = 1,
            onSuccess = { pin ->
                val hashedPin = CypherUtils.hashPasswordForVetoPin(pin)
                encSettings.setVetoPin(hashedPin)
                encSettings.setRawVetoPin(pin)
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

    private fun onEditWrongPassClicked() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText((settings.get(Settings.SET_THEFT_WRONG_PASS_ATTEMPTS) as Int).toString())
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Maximum Wrong Attempts")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val edited = input.text.toString().toIntOrNull()
                if (edited != null && edited > 0) {
                    settings.set(Settings.SET_THEFT_WRONG_PASS_ATTEMPTS, edited)
                    updateUI()
                } else {
                    Toast.makeText(this, "Please enter a valid number greater than 0", Toast.LENGTH_SHORT).show()
                }
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
