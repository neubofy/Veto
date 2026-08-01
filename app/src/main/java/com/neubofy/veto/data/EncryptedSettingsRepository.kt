package com.neubofy.veto.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.neubofy.veto.utils.SingletonHolder


/**
 * Storage for sensitive values that benefit from an additional layer of encryption.
 *
 * Because encryption is device-bound, these settings cannot (and should not) be backed up.
 */
class EncryptedSettingsRepository private constructor(context: Context) {

    companion object :
        SingletonHolder<EncryptedSettingsRepository, Context>(::EncryptedSettingsRepository) {

        val TAG = EncryptedSettingsRepository::class.simpleName

        // This file should be EXCLUDED from backups
        private const val FILENAME = "veto_encrypted_settings"

        private const val KEY_SERVER_CACHED_ACCESS_TOKEN = "KEY_SERVER_CACHED_ACCESS_TOKEN"
        private const val KEY_Veto_PIN = "KEY_Veto_PIN"
        private const val KEY_DELETE_PASSWORD = "KEY_DELETE_PASSWORD"
        private const val KEY_ALLOWLIST_JSON = "KEY_ALLOWLIST_JSON"
        private const val KEY_TEMP_ALLOWLIST_JSON = "KEY_TEMP_ALLOWLIST_JSON"
    }

    val sharedPrefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPrefs = EncryptedSharedPreferences.create(
            context,
            FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getCachedAccessToken(): String {
        return sharedPrefs.getString(KEY_SERVER_CACHED_ACCESS_TOKEN, "") ?: ""
    }

    fun setCachedAccessToken(newToken: String) {
        sharedPrefs.edit().putString(KEY_SERVER_CACHED_ACCESS_TOKEN, newToken).apply()
    }

    fun getVetoPin(): String? {
        return sharedPrefs.getString(KEY_Veto_PIN, null)
    }

    fun setVetoPin(new: String?) {
        if (new.isNullOrBlank()) {
            sharedPrefs.edit().remove(KEY_Veto_PIN).apply()
        } else {
            sharedPrefs.edit().putString(KEY_Veto_PIN, new).apply()
        }
    }

    fun getDeletePassword(): String? {
        return sharedPrefs.getString(KEY_DELETE_PASSWORD, null)
    }

    fun setDeletePassword(new: String?) {
        if (new.isNullOrBlank()) {
            sharedPrefs.edit().remove(KEY_DELETE_PASSWORD).apply()
        } else {
            val hash = if (new.startsWith("\$argon2id\$")) new else com.neubofy.veto.utils.CypherUtils.hashPasswordForDelete(new)
            sharedPrefs.edit().putString(KEY_DELETE_PASSWORD, hash).apply()
        }
    }

    fun getAllowlistJson(): String? {
        return sharedPrefs.getString(KEY_ALLOWLIST_JSON, null)
    }

    fun setAllowlistJson(json: String?) {
        if (json.isNullOrBlank()) {
            sharedPrefs.edit().remove(KEY_ALLOWLIST_JSON).apply()
        } else {
            sharedPrefs.edit().putString(KEY_ALLOWLIST_JSON, json).apply()
        }
    }

    fun getTempAllowlistJson(): String? {
        return sharedPrefs.getString(KEY_TEMP_ALLOWLIST_JSON, null)
    }

    fun setTempAllowlistJson(json: String?) {
        if (json.isNullOrBlank()) {
            sharedPrefs.edit().remove(KEY_TEMP_ALLOWLIST_JSON).apply()
        } else {
            sharedPrefs.edit().putString(KEY_TEMP_ALLOWLIST_JSON, json).apply()
        }
    }


    fun isTransportEnabled(transportKey: String): Boolean {
        return sharedPrefs.getBoolean("KEY_TRANSPORT_ENABLED_${transportKey.uppercase()}", false)
    }

    fun setTransportEnabled(transportKey: String, enabled: Boolean) {
        sharedPrefs.edit().putBoolean("KEY_TRANSPORT_ENABLED_${transportKey.uppercase()}", enabled).apply()
    }

    fun getAllowedNotificationPackages(): Set<String> {
        return sharedPrefs.getStringSet("KEY_ALLOWED_NOTIFICATION_PACKAGES", emptySet()) ?: emptySet()
    }

    fun setAllowedNotificationPackages(packages: Set<String>) {
        sharedPrefs.edit().putStringSet("KEY_ALLOWED_NOTIFICATION_PACKAGES", packages).apply()
    }
}
