package com.neubofy.veto.data

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberStrategy
import com.google.gson.stream.JsonReader
import com.google.gson.stream.MalformedJsonException
import com.neubofy.veto.utils.SingletonHolder
import com.neubofy.veto.utils.log
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter


const val SETTINGS_FILENAME = "settings.json"


// Workaround for Gson defaulting to Long or Double instead of Int.
// The underlying problem is that Settings is not a strongly typed map (it uses Object/Any)
//
// Inspired by/copied from ToNumberPolicy.LONG_OR_DOUBLE.
//
// We cannot use LONG_OR_DOUBLE because sometimes Gson does use Integers, and then our
// code cannot handle both Long and Integer. So just deserialise as Int.
object INT_OR_DOUBLE : ToNumberStrategy {
    @Throws(IOException::class, JsonParseException::class)
    override fun readNumber(`in`: JsonReader): Number {
        val value = `in`.nextString()
        return try {
            value.toInt()
        } catch (e: NumberFormatException) {
            parseAsDouble(value, `in`)
        }
    }

    @Throws(IOException::class)
    private fun parseAsDouble(value: String, `in`: JsonReader): Number {
        try {
            val d = value.toDouble()
            if ((d.isInfinite() || d.isNaN()) && !`in`.isLenient) {
                throw MalformedJsonException(
                    "JSON forbids NaN and infinities: " + d + "; at path " + `in`.previousPath
                )
            }
            return d
        } catch (e: java.lang.NumberFormatException) {
            throw JsonParseException(
                "Cannot parse " + value + "; at path " + `in`.previousPath, e
            )
        }
    }
}


/**
 * Settings should be accessed through this repository.
 * This is to only have a single Settings instance,
 * thus preventing race conditions.
 */
class SettingsRepository private constructor(private val context: Context) {

    companion object :
        SingletonHolder<SettingsRepository, Context>(::SettingsRepository) {

        val TAG = SettingsRepository::class.simpleName
    }

    private val gson = GsonBuilder()
        .setObjectToNumberStrategy(INT_OR_DOUBLE) //(ToNumberPolicy.LONG_OR_DOUBLE)
        .serializeSpecialFloatingPointValues() // to allow NaN
        .create()

    // Should only be accessed via the getters/setters in this repository
    private var settings: Settings

    init {
        settings = loadNoSet()
    }

    fun load() {
        settings = loadNoSet()
    }

    private fun loadNoSet(): Settings {
        val encSettings = EncryptedSettingsRepository.getInstance(context)
        val json = encSettings.sharedPrefs.getString("KEY_ALL_SETTINGS_JSON", null)
        
        if (json != null) {
            return gson.fromJson(json, Settings::class.java) ?: Settings()
        }

        // Migration from old plaintext file
        val file = File(context.filesDir, SETTINGS_FILENAME)
        if (file.exists()) {
            try {
                FileReader(file).use { reader ->
                    val oldSettings = gson.fromJson(reader, Settings::class.java) ?: Settings()
                    // Save to encrypted storage
                    encSettings.sharedPrefs.edit().putString("KEY_ALL_SETTINGS_JSON", gson.toJson(oldSettings)).apply()
                    // Delete old plaintext file
                    file.delete()
                    return oldSettings
                }
            } catch (e: Exception) {
                context.log().e(TAG, "Failed to migrate old settings: ${e.message}")
            }
        }

        return Settings()
    }

    private fun saveSettings() {
        val encSettings = EncryptedSettingsRepository.getInstance(context)
        encSettings.sharedPrefs.edit().putString("KEY_ALL_SETTINGS_JSON", gson.toJson(settings)).apply()
    }

    fun <T : Any> set(key: Int, value: T) {
        settings[key] = value
        saveSettings()
    }

    fun get(key: Int): Any {
        return settings.get(key)
    }

    fun remove(key: Int) {
        settings.remove(key)
        saveSettings()
    }

    fun writeAsJson(outputStreamWriter: OutputStreamWriter) {
        com.neubofy.veto.utils.writeAsJson(outputStreamWriter, gson, settings)
    }

    @Throws(JsonIOException::class, JsonSyntaxException::class)
    fun importFromStream(inputStream: InputStream) {
        val reader = JsonReader(InputStreamReader(inputStream))
        settings = gson.fromJson(reader, Settings::class.java) ?: Settings()
        saveSettings()
    }




}
