package com.neubofy.veto.utils

import android.content.Context
import android.util.Log
import com.neubofy.veto.data.LogEntry
import com.neubofy.veto.data.LogRepository


// Helper extension function for shorter code
fun Context.log() = VetoLog.getInstance(this)


/**
 * Custom logger that logs both the ADB Logcat and to a file.
 * This allows showing the logs in the UI and exporting them.
 */
class VetoLog private constructor(context: Context) {

    companion object : SingletonHolder<VetoLog, Context>(::VetoLog) {}

    private val repo = LogRepository.getInstance(context)

    fun d(tag: String?, msg: String) {
        Log.d(tag, msg)
        val now = System.currentTimeMillis()
        repo.add(LogEntry("DEBUG", now, tag ?: "", msg))
    }

    fun i(tag: String?, msg: String) {
        Log.i(tag, msg)
        val now = System.currentTimeMillis()
        repo.add(LogEntry("INFO", now, tag ?: "", msg))
    }

    fun w(tag: String?, msg: String) {
        Log.w(tag, msg)
        val now = System.currentTimeMillis()
        repo.add(LogEntry("WARN", now, tag ?: "", msg))
    }

    fun e(tag: String?, msg: String) {
        Log.e(tag, msg)
        val now = System.currentTimeMillis()
        repo.add(LogEntry("ERROR", now, tag ?: "", msg))
    }
}