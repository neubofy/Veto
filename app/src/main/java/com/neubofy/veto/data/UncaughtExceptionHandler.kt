package com.neubofy.veto.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.neubofy.veto.utils.log

class UncaughtExceptionHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private val TAG = UncaughtExceptionHandler::class.java.simpleName
        const val CRASH_MSG_HEADER = "Fatal error"

        fun initUncaughtExceptionHandler(context: Context) {
            val currentDefault = Thread.getDefaultUncaughtExceptionHandler()
            if (currentDefault !is UncaughtExceptionHandler) {
                val handler = UncaughtExceptionHandler(context, currentDefault)
                Thread.setDefaultUncaughtExceptionHandler(handler)
            }
        }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        // Log the crash
        try {
            context.log().e(TAG, createNiceCrashLog(e))
        } catch (_: Exception) {}

        // Record crash to Firebase Crashlytics
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
        } catch (_: Exception) {}

        // Set the flag so that when the user launches the app again, crash details are handled
        try {
            val repo = SettingsRepository.getInstance(context)
            repo.set(Settings.SET_APP_CRASHED_LOG_ENTRY, 1)
        } catch (_: Exception) {}

        // Delegate to system default handler to terminate process cleanly instead of freezing on a blank screen
        defaultHandler?.uncaughtException(t, e)
    }

    private fun createNiceCrashLog(e: Throwable): String {
        val report = StringBuilder(CRASH_MSG_HEADER)
        report.appendLine()

        report.appendLine("--------- Stack trace ---------")
        report.appendLine()
        report.appendLine(e.toString())
        report.appendLine()
        for (ele in e.stackTrace) {
            report.appendLine(ele.toString())
        }
        report.appendLine()

        e.cause?.let { cause ->
            report.appendLine("--------- Cause ---------")
            report.appendLine()
            report.appendLine(cause.toString())
            for (ele in cause.stackTrace) {
                report.appendLine(ele.toString())
            }
            report.appendLine()
        }

        report.appendLine("--------- Device ---------")
        report.appendLine()
        report.appendLine("Brand: ${Build.BRAND}")
        report.appendLine("Device: ${Build.DEVICE}")
        report.appendLine("Model: ${Build.MODEL}")
        report.appendLine("Id: ${Build.ID}")
        report.appendLine("Product: ${Build.PRODUCT}")
        report.appendLine()

        report.appendLine("--------- Firmware ---------")
        report.appendLine()
        report.appendLine("SDK: ${Build.VERSION.SDK_INT}")
        report.appendLine("Release: ${Build.VERSION.RELEASE}")
        report.appendLine("Incremental: ${Build.VERSION.INCREMENTAL}")
        report.appendLine("Veto-Version: ${getAppVersion()}")
        report.appendLine()

        return report.toString()
    }

    private fun getAppVersion(): String {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            return info.versionName ?: "??"
        } catch (nameNotFoundException: PackageManager.NameNotFoundException) {
            return "??"
        }
    }
}
