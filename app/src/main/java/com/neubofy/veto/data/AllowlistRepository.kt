package com.neubofy.veto.data

import android.content.Context
import android.telephony.PhoneNumberUtils
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import com.neubofy.veto.R
import com.neubofy.veto.utils.Notifications
import com.neubofy.veto.utils.Notifications.CHANNEL_FAILED
import com.neubofy.veto.utils.SingletonHolder
import com.neubofy.veto.utils.log
import java.io.File
import java.io.FileReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.LinkedList


const val ALLOWLIST_FILENAME = "whitelist.json"


@Keep
class AllowlistModel : LinkedList<Contact>()


class AllowlistRepository private constructor(private val context: Context) {

    companion object : SingletonHolder<AllowlistRepository, Context>(::AllowlistRepository) {
        val TAG = AllowlistRepository::class.simpleName
    }

    private val gson = Gson()

    var list: AllowlistModel
        private set

    init {
        val file = File(context.filesDir, ALLOWLIST_FILENAME)
        if (!file.exists()) {
            file.createNewFile()
        }
        val reader = JsonReader(FileReader(file))
        list = try {
            gson.fromJson(reader, AllowlistModel::class.java) ?: AllowlistModel()
        } catch (e: JsonSyntaxException) {
            context.log().e(TAG, e.stackTraceToString())
            // Reset the list
            notifyAllowlistReset()
            AllowlistModel()
        }
    }

    private fun saveList() {
        val copiedList = list.clone()
        val raw = gson.toJson(copiedList)
        val file = File(context.filesDir, ALLOWLIST_FILENAME)
        file.writeText(raw)
    }

    fun writeAsJson(outputStreamWriter: OutputStreamWriter) {
        com.neubofy.veto.utils.writeAsJson(outputStreamWriter, gson, list)
    }

    @Throws(JsonIOException::class, JsonSyntaxException::class)
    fun importFromStream(inputStream: InputStream) {
        val reader = JsonReader(InputStreamReader(inputStream))
        list = gson.fromJson(reader, AllowlistModel::class.java) ?: AllowlistModel()
        saveList()
    }

    fun contains(c: Contact): Boolean {
        return containsNumber(c.number)
    }

    fun containsNumber(number: String): Boolean {
        for (ele in list) {
            if (PhoneNumberUtils.compare(ele.number, number)) {
                return true
            }
        }
        return false
    }

    fun add(c: Contact) {
        if (!contains(c)) {
            list.add(c)
            saveList()
        }
    }

    fun remove(phoneNumber: String) {
        val toRemove = mutableListOf<Contact>()
        for (ele in list) {
            if (PhoneNumberUtils.compare(ele.number, phoneNumber)) {
                toRemove.add(ele)
            }
        }
        list.removeAll(toRemove)
        saveList()
    }

    private fun notifyAllowlistReset() {
        val title = context.getString(R.string.allowlist_reset_title)
        val text = context.getString(R.string.allowlist_reset_text)
        Notifications.notify(context, title, text, CHANNEL_FAILED)
    }
}
