package com.neubofy.veto.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.neubofy.veto.R
import com.neubofy.veto.utils.UpdateManager
import com.neubofy.veto.utils.log
import io.noties.markwon.Markwon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class AboutActivity : AppCompatActivity() {

    companion object {
        const val ABOUT_MD_URL = "https://raw.githubusercontent.com/neubofy/Vito/main/ABOUT.md"
        const val GITHUB_PROFILE = "https://github.com/pawanwashudev-official"
        const val WEBSITE = "https://veto.neubofy.in"
        const val EMAIL = "support@neubofy.in"
        private val TAG = AboutActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { finish() }

        val tvVersion = findViewById<TextView>(R.id.tvVersion)
        tvVersion.text = "Version " + com.neubofy.veto.BuildConfig.VERSION_NAME

        // Social Links
        findViewById<LinearLayout>(R.id.btnGithub).setOnClickListener { openUrl(GITHUB_PROFILE) }
        findViewById<LinearLayout>(R.id.btnWebsite).setOnClickListener { openUrl(WEBSITE) }

        // Email / Contact
        findViewById<LinearLayout>(R.id.btnContact).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$EMAIL")
                putExtra(Intent.EXTRA_SUBJECT, "Veto App - Support Request")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Email", EMAIL)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }

        // Update Check
        val progressUpdate = findViewById<CircularProgressIndicator>(R.id.progressUpdate)
        val tvUpdateStatus = findViewById<TextView>(R.id.tvUpdateStatus)
        findViewById<MaterialCardView>(R.id.cardUpdate).setOnClickListener {
            progressUpdate.visibility = View.VISIBLE
            tvUpdateStatus.text = "Checking for updates..."
            
            UpdateManager.checkForUpdates(this, silent = false, isBeta = false, onCheckComplete = {
                runOnUiThread {
                    progressUpdate.visibility = View.GONE
                    tvUpdateStatus.text = "Keep Veto at its best"
                }
            })
        }

        // Beta Update Check
        val progressBetaUpdate = findViewById<CircularProgressIndicator>(R.id.progressBetaUpdate)
        val tvBetaUpdateStatus = findViewById<TextView>(R.id.tvBetaUpdateStatus)
        findViewById<MaterialCardView>(R.id.cardBetaUpdate).setOnClickListener {
            progressBetaUpdate.visibility = View.VISIBLE
            tvBetaUpdateStatus.text = "Checking for beta updates..."
            
            UpdateManager.checkForUpdates(this, silent = false, isBeta = true, onCheckComplete = {
                runOnUiThread {
                    progressBetaUpdate.visibility = View.GONE
                    tvBetaUpdateStatus.text = "Get early access features"
                }
            })
        }

        loadAboutContent()
    }

    private fun loadAboutContent() {
        val prefs = getSharedPreferences("about_cache", android.content.Context.MODE_PRIVATE)
        val cachedContent = prefs.getString("markdown_content", null)
        val tvMarkdownContent = findViewById<TextView>(R.id.tvMarkdownContent)

        val markwon = Markwon.builder(this).build()

        if (cachedContent != null) {
            markwon.setMarkdown(tvMarkdownContent, cachedContent)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val content = URL(ABOUT_MD_URL).readText()
                withContext(Dispatchers.Main) {
                    markwon.setMarkdown(tvMarkdownContent, content)
                    prefs.edit().putString("markdown_content", content).apply()
                }
            } catch (e: Exception) {
                log().e(TAG, "Failed to load markdown: \${e.message}")
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }
}
