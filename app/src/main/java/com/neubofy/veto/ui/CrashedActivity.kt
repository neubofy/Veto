package com.neubofy.veto.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.neubofy.veto.R
import com.neubofy.veto.data.LogRepository
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.utils.Utils

class CrashedActivity : VetoActivity() {

    private var crashLog: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crashed)

        UiUtil.setupEdgeToEdgeAppBar(findViewById(R.id.appBar))
        UiUtil.setupEdgeToEdgeScrollView(findViewById(R.id.scrollView))

        val settings = SettingsRepository.getInstance(this)
        settings.set(Settings.SET_APP_CRASHED_LOG_ENTRY, 0)

        val repo = LogRepository.getInstance(this)
        val entry = repo.getLastCrashLog()
        if (entry == null) {
            continueToMain()
            return
        }
        crashLog = entry.msg

        val textViewCrashLog = findViewById<TextView>(R.id.textViewCrash)
        textViewCrashLog.text = crashLog

        val buttonSendLog = findViewById<Button>(R.id.buttonSendLog)
        buttonSendLog.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/neubofy/Veto/issues"))
            startActivity(intent)
            finish()
        }

        val buttonCopy = findViewById<Button>(R.id.buttonCopyLog)
        buttonCopy.setOnClickListener { v ->
            Utils.copyToClipboard(v.context, "CrashLog", crashLog ?: "")
        }

        val buttonContinue = findViewById<Button>(R.id.buttonContinue)
        buttonContinue.setOnClickListener {
            continueToMain()
        }
    }

    private fun continueToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
