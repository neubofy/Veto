package com.neubofy.veto.data

import android.content.Context
import android.telephony.PhoneNumberUtils
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import com.neubofy.veto.utils.SingletonHolder
import com.neubofy.veto.utils.log
import java.io.File
import java.io.FileReader
import java.util.LinkedList


const val TEMP_ALLOWLIST_FILENAME = "temporary_allowlist.json"
const val TEMP_USAGE_VALIDITY_MILLIS = 10 * 60 * 1000 // 10 min


@Keep
data class TempAllowedNumber(
    val number: String,
    val subscriptionId: Int,
    val createdTimeMillis: Long,
) {
    fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        return createdTimeMillis + TEMP_USAGE_VALIDITY_MILLIS < now
    }
}

@Keep
class TemporaryAllowlistModel : LinkedList<TempAllowedNumber>()


class TemporaryAllowlistRepository private constructor(private val context: Context) {

    companion object :
        SingletonHolder<TemporaryAllowlistRepository, Context>(::TemporaryAllowlistRepository) {

        val TAG = TemporaryAllowlistRepository::class.simpleName
    }

    private val gson = Gson()
    private val encRepo = EncryptedSettingsRepository.getInstance(context)

    private val list: TemporaryAllowlistModel

    init {
        val encryptedJson = encRepo.getTempAllowlistJson()
        if (encryptedJson != null) {
            list = try {
                gson.fromJson(encryptedJson, TemporaryAllowlistModel::class.java) ?: TemporaryAllowlistModel()
            } catch (e: JsonSyntaxException) {
                context.log().e(TAG, e.stackTraceToString())
                TemporaryAllowlistModel()
            }
        } else {
            val legacyFile = File(context.filesDir, TEMP_ALLOWLIST_FILENAME)
            if (legacyFile.exists() && legacyFile.length() > 0) {
                list = try {
                    val reader = JsonReader(FileReader(legacyFile))
                    gson.fromJson(reader, TemporaryAllowlistModel::class.java) ?: TemporaryAllowlistModel()
                } catch (e: JsonSyntaxException) {
                    context.log().e(TAG, e.stackTraceToString())
                    TemporaryAllowlistModel()
                }
                saveList()
                try { legacyFile.delete() } catch (_: Exception) {}
            } else {
                list = TemporaryAllowlistModel()
            }
        }
    }

    private fun saveList() {
        val copiedList = list.clone()
        val raw = gson.toJson(copiedList)
        encRepo.setTempAllowlistJson(raw)
    }

    fun containsValidNumber(number: String): Boolean {
        for (ele in list) {
            if (PhoneNumberUtils.compare(ele.number, number)) {
                if (ele.isExpired()) {
                    list.remove(ele)
                    saveList()
                } else {
                    return true
                }
            }
        }
        return false
    }

    fun add(number: String, subscriptionId: Int) {
        val now = System.currentTimeMillis()

        for (ele in list) {
            if (PhoneNumberUtils.compare(ele.number, number)) {
                val new = ele.copy(createdTimeMillis = now)
                list.remove(ele)
                list.add(new)
                saveList()
                return
            }
        }

        // If it was not in the list
        list.add(TempAllowedNumber(number, subscriptionId, now))
        saveList()
    }

    fun getList(): List<TempAllowedNumber> {
        return list
    }

    fun remove(phoneNumber: String) {
        val toRemove = mutableListOf<TempAllowedNumber>()
        for (ele in list) {
            if (PhoneNumberUtils.compare(ele.number, phoneNumber)) {
                toRemove.add(ele)
            }
        }
        list.removeAll(toRemove)
        saveList()
    }

    /**
     * Removes all entries that have expired from the temporary allowlist.
     */
    fun removeExpired(): List<Pair<String, Int>> {
        val toRemove = mutableListOf<TempAllowedNumber>()
        for (ele in list) {
            if (ele.isExpired()) {
                toRemove.add(ele)
            }
        }
        list.removeAll(toRemove)
        saveList()
        return toRemove.map { it.number to it.subscriptionId }
    }
}
