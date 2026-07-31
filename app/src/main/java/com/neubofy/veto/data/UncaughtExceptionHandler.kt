package com.neubofy.veto.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.neubofy.veto.utils.log


class UncaughtExceptionHandler(
    private val context: Context
) : Thread.UncaughtExceptionHandler {

    companion object {
        private val TAG = UncaughtExceptionHandler::class.java.simpleName

        const val CRASH_MSG_HEADER = "Fatal error"

        fun initUncaughtExceptionHandler(context: Context) {
            val handler = UncaughtExceptionHandler(context)
            Thread.currentThread().setUncaughtExceptionHandler(handler)
        }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        // Log the crash
        context.log().e(TAG, createNiceCrashLog(e))

        // Set the flag so that when the user launches the app again, the crash details are shown
        val repo = SettingsRepository.getInstance(context)
        repo.set(Settings.SET_APP_CRASHED_LOG_ENTRY, 1)

        // Don't show the CrashedActivity now (it often fails)
    }

    private fun createNiceCrashLog(e: Throwable): String {
        // Must start with this header for getLastCrashLog() to work
        val report = StringBuffer(CRASH_MSG_HEADER)
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
            nameNotFoundException.printStackTrace()
            return "??"
        }
    }
}
