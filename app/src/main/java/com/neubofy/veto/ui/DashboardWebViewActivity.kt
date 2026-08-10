package com.neubofy.veto.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.google.android.material.appbar.MaterialToolbar
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository

class DashboardWebViewActivity : VetoActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_webview)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val webView = findViewById<WebView>(R.id.webView)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = newProgress
                }
            }
        }

        val settings = SettingsRepository.getInstance(this)
        var url = settings.get(Settings.SET_VetoSERVER_URL) as String
        if (url.isEmpty()) {
            url = "https://veto.neubofy.in/dashboard"
        } else {
            if (!url.endsWith("/")) url += "/"
            url += "dashboard"
        }
        
        webView.loadUrl(url)
    }
}
