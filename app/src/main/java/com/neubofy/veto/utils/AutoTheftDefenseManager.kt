package com.neubofy.veto.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import com.neubofy.veto.commands.*
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.transports.AutoTheftTransport
import com.neubofy.veto.transports.InAppTransport
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

object AutoTheftDefenseManager : TextToSpeech.OnInitListener {
    private const val TAG = "AutoTheftDefenseManager"
    private const val FIVE_MINUTES_MS = 5 * 60 * 1000L

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingMessage: String? = null

    // Terminal log buffer (max 20 entries)
    private val logBuffer = Collections.synchronizedList(mutableListOf<String>())
    private var onLogUpdatedListener: (() -> Unit)? = null

    fun setLogListener(listener: (() -> Unit)?) {
        onLogUpdatedListener = listener
    }

    fun getLogs(): List<String> {
        return synchronized(logBuffer) { ArrayList(logBuffer) }
    }

    fun logTerminal(context: Context, tag: String, message: String) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logLine = "[$timeStr] $message"
        synchronized(logBuffer) {
            logBuffer.add(logLine)
            if (logBuffer.size > 20) {
                logBuffer.removeAt(0)
            }
        }
        context.log().d(tag, logLine)
        onLogUpdatedListener?.invoke()
    }

    fun clearTerminal() {
        synchronized(logBuffer) { logBuffer.clear() }
        onLogUpdatedListener?.invoke()
    }

    /**
     * Engage System Trackable Condition Toggles using in-app commands.
     */
    suspend fun applyTrackableConditionToggles(context: Context) {
        val inApp = InAppTransport(context)
        logTerminal(context, TAG, "Engaging trackable conditions...")

        try {
            // 1. Disable DND
            NoDisturbCommand(context).execute(listOf("off"), inApp)
            logTerminal(context, TAG, "DND Filter -> OFF (Allow All)")
        } catch (e: Exception) {
            logTerminal(context, TAG, "DND Toggle Failed: ${e.message}")
        }

        try {
            // 2. Set Ringer Mode to NORMAL & 100% Volume
            RingerModeCommand(context).execute(listOf("normal"), inApp)
            logTerminal(context, TAG, "Ringer Mode -> NORMAL (100% Volume)")
        } catch (e: Exception) {
            logTerminal(context, TAG, "Ringer Toggle Failed: ${e.message}")
        }

        try {
            // 3. Turn GPS ON
            GpsCommand(context).execute(listOf("on"), inApp)
            logTerminal(context, TAG, "GPS Satellite Hardware -> ENGAGED")
        } catch (e: Exception) {
            logTerminal(context, TAG, "GPS Toggle Failed: ${e.message}")
        }

        // Add terrifying BLE Mesh status lines
        logTerminal(context, TAG, "ESTABLISHING BLE MESH PEER TRACKING...")
        delay(500)
        logTerminal(context, TAG, "BLE MESH LINK CONNECTED (4 PEERS ACQUIRED)")
        logTerminal(context, TAG, "CONTINUOUS TELEMETRY & POLICE BEACON ACTIVE")
    }

    /**
     * Executes the Bad Event command chain (locate, stats, photo front, photo back)
     * using standard in-app commands over AutoTheftTransport, throttled to 5 minutes.
     */
    suspend fun executeBadEventCommandChain(context: Context, force: Boolean = false) {
        val settings = SettingsRepository.getInstance(context)
        val lastRun = settings.get(Settings.SET_AUTO_THEFT_LAST_BAD_EVENT_TIME) as? Long ?: 0L
        val now = System.currentTimeMillis()

        if (!force && (now - lastRun < FIVE_MINUTES_MS)) {
            val remainingSecs = ((FIVE_MINUTES_MS - (now - lastRun)) / 1000)
            logTerminal(context, TAG, "Bad event command chain throttled ($remainingSecs s remaining)")
            return
        }

        settings.set(Settings.SET_AUTO_THEFT_LAST_BAD_EVENT_TIME, now)
        val transport = AutoTheftTransport(context)

        CommandQueueManager.runMediaCommandInQueue {
            logTerminal(context, TAG, "Executing Bad Event Command Chain...")

            try {
                logTerminal(context, TAG, "Executing $ locate")
                LocateCommand(context).execute(emptyList(), transport)
            } catch (e: Exception) {
                logTerminal(context, TAG, "Locate command failed: ${e.message}")
            }

            try {
                logTerminal(context, TAG, "Executing $ stats")
                StatsCommand(context).execute(emptyList(), transport)
            } catch (e: Exception) {
                logTerminal(context, TAG, "Stats command failed: ${e.message}")
            }

            try {
                logTerminal(context, TAG, "Executing $ photo front flash")
                CameraCommand(context).execute(listOf("front", "flash"), transport)
            } catch (e: Exception) {
                logTerminal(context, TAG, "Front photo failed: ${e.message}")
            }

            try {
                logTerminal(context, TAG, "Executing $ photo back flash")
                CameraCommand(context).execute(listOf("back", "flash"), transport)
            } catch (e: Exception) {
                logTerminal(context, TAG, "Back photo failed: ${e.message}")
            }

            logTerminal(context, TAG, "Command Chain Complete. Telemetry/Photos Exfiltrated.")
        }
    }

    fun speakWarning(context: Context, message: String) {
        pendingMessage = message
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        } else if (isTtsInitialized) {
            speakRepeatedly(message)
        }
    }

    fun speakTestTts(context: Context, message: String) {
        speakWarning(context, message)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsInitialized = true
            pendingMessage?.let {
                speakRepeatedly(it)
                pendingMessage = null
            }
        }
    }

    private fun speakRepeatedly(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "AutoTheftTTS")
        tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "AutoTheftTTS")
        tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "AutoTheftTTS")
    }

    fun stopTts() {
        tts?.stop()
    }
}
