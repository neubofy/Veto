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
            sharedPrefs.edit().putString(KEY_DELETE_PASSWORD, new).apply()
        }
    }
}
