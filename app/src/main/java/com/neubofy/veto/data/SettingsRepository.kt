package com.neubofy.veto.data

import android.content.Context
import androidx.core.net.toUri
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberStrategy
import com.google.gson.stream.JsonReader
import com.google.gson.stream.MalformedJsonException
import com.neubofy.veto.BuildConfig
import com.neubofy.veto.R
import com.neubofy.veto.utils.CypherUtils
import com.neubofy.veto.utils.SingletonHolder
import com.neubofy.veto.utils.Utils
import com.neubofy.veto.utils.log
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.EncodedKeySpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec


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

    fun <T> set(key: Int, value: T) {
        settings.set(key, value)
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

    fun migrateSettings() {
        val currentVersion = get(Settings.SET_SET_VERSION) as Int


        if (currentVersion < 3) {
            migrateDeletePassword()
        }
        migrateBackgroundLocationType()

        set(Settings.SET_SET_VERSION, Settings.SETTINGS_VERSION)
    }



    private fun migrateDeletePassword() {
        // For users that upgrade, initialize the new delete password with the existing Veto PIN
        context.log().i(TAG, "Migrating to separate delete password")
        val encSettings = EncryptedSettingsRepository.getInstance(context)
        val pin = encSettings.getVetoPin()
        encSettings.setDeletePassword(pin)
    }

    private fun migrateBackgroundLocationType() {
        val oldType = get(Settings.SET_VetoSERVER_LOCATION_TYPE) as Int
        if (oldType < BackgroundLocationType.BASE) {
            val newType = BackgroundLocationType.fromOldEncoding(oldType)
            set(Settings.SET_VetoSERVER_LOCATION_TYPE, newType.encode())
        }
    }

// ---------- Convenience helpers ----------

    fun serverAccountExists(): Boolean {
        val id = get(Settings.SET_VetoSERVER_ID) as String
        return id.isNotEmpty()
    }

    fun setKeys(keys: VetoKeyPair) {
        set(Settings.SET_Veto_CRYPT_PRIVKEY, keys.encryptedPrivateKey)
        set(Settings.SET_Veto_CRYPT_PUBKEY, CypherUtils.encodeBase64(keys.publicKey.encoded))
    }

    fun getKeys(): VetoKeyPair? {
        if (get(Settings.SET_Veto_CRYPT_PUBKEY) == "") {
            return null
        }

        val pubKeySpec: EncodedKeySpec = X509EncodedKeySpec(
            CypherUtils.decodeBase64(get(Settings.SET_Veto_CRYPT_PUBKEY) as String)
        )
        var publicKey: PublicKey? = null
        try {
            val keyFactory = KeyFactory.getInstance("RSA")
            publicKey = keyFactory.generatePublic(pubKeySpec)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        }

        return if (publicKey != null) {
            VetoKeyPair(publicKey, get(Settings.SET_Veto_CRYPT_PRIVKEY) as String)
        } else {
            null
        }
    }

    fun removeServerAccount() {
        set(Settings.SET_VetoSERVER_ID, "")
        set(Settings.SET_Veto_CRYPT_HPW, "")
        set(Settings.SET_Veto_CRYPT_PRIVKEY, "")
        set(Settings.SET_Veto_CRYPT_PUBKEY, "")
    }

    fun storeLastKnownLocation(loc: VetoLocation) {
        // historically stored as String
        set<String>(Settings.SET_LAST_KNOWN_LOCATION_LAT, loc.lat.toString())
        set<String>(Settings.SET_LAST_KNOWN_LOCATION_LON, loc.lon.toString())

        if (loc.accuracy != null) {
            set<Float>(Settings.SET_LAST_KNOWN_LOCATION_ACCURACY, loc.accuracy)
        } else {
            set<Float>(Settings.SET_LAST_KNOWN_LOCATION_ACCURACY, Float.NaN)
        }
        if (loc.altitude != null) {
            set<Double>(Settings.SET_LAST_KNOWN_LOCATION_ALTITUDE, loc.altitude)
        } else {
            set<Double>(Settings.SET_LAST_KNOWN_LOCATION_ALTITUDE, Double.NaN)
        }
        if (loc.bearing != null) {
            set<Float>(Settings.SET_LAST_KNOWN_LOCATION_BEARING, loc.bearing)
        } else {
            set<Float>(Settings.SET_LAST_KNOWN_LOCATION_BEARING, Float.NaN)
        }
        if (loc.speed != null) {
            set<Float>(Settings.SET_LAST_KNOWN_LOCATION_SPEED, loc.speed)
        } else {
            set<Float>(Settings.SET_LAST_KNOWN_LOCATION_SPEED, Float.NaN)
        }

        set<Long>(Settings.SET_LAST_KNOWN_LOCATION_TIME, loc.timeMillis)
    }

    /**
     * Return the last known location as cached by the settings.
     */
    fun getLastKnownLocation(): VetoLocation? {
        return try {
            val loc = getLastKnownLocationInt()
            loc
        } catch (e: NumberFormatException) {
            null
        } catch (e: ClassCastException) {
            // https://gitlab.com/veto-foss/veto-android/-/issues/379
            // java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Number
            null
        }
    }

    private fun getLastKnownLocationInt(): VetoLocation {
        var acc: Float? = (get(Settings.SET_LAST_KNOWN_LOCATION_ACCURACY) as Number).toFloat()
        if (acc!!.isNaN()) {
            acc = null
        }
        var alt: Double? = (get(Settings.SET_LAST_KNOWN_LOCATION_ALTITUDE) as Number).toDouble()
        if (alt!!.isNaN()) {
            alt = null
        }
        var bear: Float? = (get(Settings.SET_LAST_KNOWN_LOCATION_BEARING) as Number).toFloat()
        if (bear!!.isNaN()) {
            bear = null
        }
        var speed: Float? = (get(Settings.SET_LAST_KNOWN_LOCATION_SPEED) as Number).toFloat()
        if (speed!!.isNaN()) {
            speed = null
        }

        return VetoLocation(
            lat = (get(Settings.SET_LAST_KNOWN_LOCATION_LAT) as String).toDouble(),
            lon = (get(Settings.SET_LAST_KNOWN_LOCATION_LON) as String).toDouble(),
            accuracy = acc,
            altitude = alt,
            bearing = bear,
            speed = speed,
            provider = context.getString(R.string.cmd_locate_last_known_location_text),
            batteryLevel = Utils.getBatteryLevel(context),
            timeMillis = (get(Settings.SET_LAST_KNOWN_LOCATION_TIME) as Number).toLong(),
        )
    }
}
