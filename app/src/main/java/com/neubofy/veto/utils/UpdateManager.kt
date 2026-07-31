package com.neubofy.veto.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import com.azhon.appupdate.manager.DownloadManager
import com.neubofy.veto.BuildConfig
import com.neubofy.veto.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Handle professional app updates via GitHub Releases.
 * Features: 7-day automatic check throttle, manual check, and professional in-app installation.
 */
object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val PREFS_NAME = "update_prefs"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"
    private const val CHECK_INTERVAL_DAYS = 7L

    private const val GITHUB_API_URL = "https://api.github.com/repos/neubofy/Veto/releases"

    /**
     * Check for updates.
     * @param silent If true, only checks if 7 days have passed since last check.
     */
    fun checkForUpdates(context: Context, silent: Boolean = true, isBeta: Boolean = false, onCheckComplete: (() -> Unit)? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
        val now = System.currentTimeMillis()

        if (silent && now - lastCheck < TimeUnit.DAYS.toMillis(CHECK_INTERVAL_DAYS)) {
            Log.d(TAG, "Update check skipped (7-day throttle active)")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val releases = fetchGitHubReleases()
                if (releases != null) {
                    var bestRelease: JSONObject? = null
                    var bestApkUrl = ""
                    var maxTimestamp = 0L
                    var bestVersion = ""
                    var bestNotes = ""

                    val regex = Regex(".*?(\\d{13,}).*?\\.apk")

                    for (r in 0 until releases.length()) {
                        val release = releases.getJSONObject(r)
                        val isPrerelease = release.getBoolean("prerelease")

                        if (isPrerelease == isBeta) {
                            val assetsArray = release.getJSONArray("assets")
                            val version = release.getString("tag_name").replace("v", "")

                            for (i in 0 until assetsArray.length()) {
                                val asset = assetsArray.getJSONObject(i)
                                val name = asset.getString("name")
                                if (name.endsWith(".apk")) {
                                    val match = regex.find(name)
                                    if (match != null) {
                                        val timestamp = match.groupValues[1].toLongOrNull() ?: 0L
                                        if (timestamp > maxTimestamp) {
                                            maxTimestamp = timestamp
                                            bestApkUrl = asset.getString("browser_download_url")
                                            bestRelease = release
                                            bestVersion = version
                                            bestNotes = release.getString("body")
                                        }
                                    } else {
                                        // Fallback: if no timestamp but it's the first matching release we see (which is newest by date)
                                        if (bestApkUrl.isEmpty() && bestRelease == null) {
                                            bestApkUrl = asset.getString("browser_download_url")
                                            bestRelease = release
                                            bestVersion = version
                                            bestNotes = release.getString("body")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (bestRelease != null) {
                        val isEligible = (maxTimestamp > 0 && maxTimestamp > BuildConfig.BUILD_TIMESTAMP) ||
                                         (maxTimestamp == 0L && isNewerVersion(bestVersion, BuildConfig.VERSION_NAME))

                        if (isEligible && bestApkUrl.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                onCheckComplete?.invoke()
                                showUpdateDialog(context, bestVersion, bestApkUrl, bestNotes)
                            }
                        } else {
                            if (!silent) withContext(Dispatchers.Main) { onCheckComplete?.invoke() }
                        }
                    } else {
                        if (!silent) withContext(Dispatchers.Main) { onCheckComplete?.invoke() }
                    }
                } else {
                    if (!silent) withContext(Dispatchers.Main) { onCheckComplete?.invoke() }
                }
                prefs.edit().putLong(KEY_LAST_CHECK_TIME, now).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Update check failed", e)
                if (!silent) withContext(Dispatchers.Main) { onCheckComplete?.invoke() }
            }
        }
    }

    private fun fetchGitHubReleases(): JSONArray? {
        return try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val content = connection.inputStream.bufferedReader().use { it.readText() }
            JSONArray(content)
        } catch (e: Exception) {
            null
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        return try {
            val latestParts = latest.split(".").map { it.toInt() }
            val currentParts = current.split(".").map { it.toInt() }
            
            for (i in 0 until minOf(latestParts.size, currentParts.size)) {
                if (latestParts[i] > currentParts[i]) return true
                if (latestParts[i] < currentParts[i]) return false
            }
            latestParts.size > currentParts.size
        } catch (e: Exception) {
            latest != current // Fallback to string comparison
        }
    }

    private fun showUpdateDialog(context: Context, version: String, url: String, notes: String) {
        if (context !is Activity) {
            Log.e(TAG, "Context must be an Activity to show update dialog")
            return
        }
        
        AlertDialog.Builder(context)
            .setTitle("Update Available")
            .setMessage("Version \$version is available.\n\nRelease Notes:\n\$notes\n\nDo you want to update now?")
            .setPositiveButton("Update") { _, _ ->
                // azhon AppUpdate manager handles the professional UI and installation prompt
                val manager = DownloadManager.Builder(context).apply {
                    apkUrl(url)
                    apkName("Veto-v\$version.apk")
                    smallIcon(R.mipmap.ic_launcher)
                    apkVersionName(version)
                    apkDescription(notes)
                    // The library automatically handles REQUEST_INSTALL_PACKAGES flow
                }.build()
                manager.download()
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
