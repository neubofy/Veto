package com.neubofy.veto.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object WebUtils {

    /**
     * Attempts to open the given URL in a Chrome Custom Tab.
     * If the user doesn't have a Custom Tabs compatible browser, it falls back
     * to a standard ACTION_VIEW intent.
     */
    fun openCustomTab(context: Context, url: String) {
        val uri = Uri.parse(url)
        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            // Fallback to standard intent if Custom Tabs fails (e.g. no suitable browser found)
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } catch (ex: Exception) {
                // If there's literally no web browser installed, there's not much we can do
                ex.printStackTrace()
            }
        }
    }
}
