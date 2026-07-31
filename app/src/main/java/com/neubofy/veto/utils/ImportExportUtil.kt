package com.neubofy.veto.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonWriter
import com.neubofy.veto.R
import com.neubofy.veto.data.ALLOWLIST_FILENAME
import com.neubofy.veto.data.AllowlistRepository
import com.neubofy.veto.data.SETTINGS_FILENAME
import com.neubofy.veto.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


private const val TAG = "ImportExportUtil"


class SettingsImportExporter(
    private val context: Context,
) {
    companion object {
        fun filenameForExport(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                "veto-settings-$date.zip"
            } else {
                "veto-settings.zip"
            }
        }
    }

    suspend fun exportData(uri: Uri) {
        writeToUri(context, uri) { outputStream ->
            val zipOutputStream = ZipOutputStream(outputStream)

            var entry = ZipEntry(SETTINGS_FILENAME)
            zipOutputStream.putNextEntry(entry)
            var writer = zipOutputStream.writer()
            SettingsRepository.getInstance(context).writeAsJson(writer)
            writer.flush()
            zipOutputStream.closeEntry()

            entry = ZipEntry(ALLOWLIST_FILENAME)
            zipOutputStream.putNextEntry(entry)
            writer = zipOutputStream.writer()
            AllowlistRepository.getInstance(context).writeAsJson(writer)
            writer.flush()
            zipOutputStream.closeEntry()

            zipOutputStream.close()
        }
    }

    @Throws(JsonIOException::class, JsonSyntaxException::class)
    suspend fun importData(uri: Uri) = withContext(Dispatchers.IO) {
        // We cannot use the file ending (.zip/.json) to detect the file,
        // since the URI path may not have those (e.g. when the file comes from Nextcloud as a content provider).
        // Thus we just need to try one format, and if it fails, try the other.

        // Old "settings.json"
        var inputStream: InputStream? = null
        try {
            context.log().i(TAG, "Trying to import as JSON")
            inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext

            SettingsRepository.getInstance(context).importFromStream(inputStream)

            withContext(Dispatchers.Main) {
                val text = context.getString(R.string.Settings_Import_Success)
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }

            // Apparently the import was successful
            return@withContext
        } catch (e: Exception) {
            // continue
        } finally {
            inputStream?.close()
        }

        // We need to open a new stream (or would need to reset the old stream).
        // Otherwise the ZIP reader starts reading wherever the JSON reader stopped.

        // New format, "veto-export.zip" (contains settings.json and other data)
        try {
            context.log().i(TAG, "Trying to import ZIP")
            inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext

            val zipInputStream = ZipInputStream(inputStream)
            var settingsFound = false
            var allowlistFound = false

            while (true) {
                val entry = zipInputStream.nextEntry ?: break

                when (entry.name) {
                    SETTINGS_FILENAME -> {
                        context.log().i(TAG, "Trying to import $SETTINGS_FILENAME")
                        SettingsRepository.getInstance(context).importFromStream(zipInputStream)
                        settingsFound = true
                    }

                    ALLOWLIST_FILENAME -> {
                        context.log().i(TAG, "Trying to import $ALLOWLIST_FILENAME")
                        AllowlistRepository.getInstance(context)
                            .importFromStream(zipInputStream)
                        allowlistFound = true
                    }

                    else -> {
                        context.log().w(TAG, "Unknown entry '${entry.name}'")
                    }
                }
            }

            if (settingsFound && allowlistFound) {
                withContext(Dispatchers.Main) {
                    val text = context.getString(R.string.Settings_Import_Success)
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                }
                return@withContext
            }

            // Consider this an import failure
            context.log().w(TAG, "ZIP: settingsFound=$settingsFound allowlistFound=$allowlistFound")

        } catch (e: Exception) {
            context.log().e(TAG, "ZIP import failed:\n${e.stackTraceToString()}")
            // continue
        } finally {
            inputStream?.close()
        }

        withContext(Dispatchers.Main) {
            val text = context.getString(R.string.Settings_Import_Failed)
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}

fun writeAsJson(
    outputStreamWriter: OutputStreamWriter,
    // Receive the Gson as parameter, to ensure it is configured with the settings that src needs
    gson: Gson,
    src: Any,
) {
    val type = src.javaClass
    val writer = JsonWriter(outputStreamWriter)
    gson.toJson(src, type, writer)

    // Don't close the JsonWriter, as this also closes all of the underlying writers.
    // This would close the entire ZIP file during settings export.
    // Callers of this function should close the outputStreamWriter themselves.
}

// Coroutines: read/write must happen on the IO thread.
// Because when the URI is backed by a remote location (e.g. Nextcloud) we otherwise get a NetworkOnMainThreadException.
// Switch back to the main thread for UI-related tasks (showing a Toast).

suspend fun writeToUri(
    context: Context,
    uri: Uri,
    write: (OutputStream) -> Unit,
) = withContext(Dispatchers.IO) {
    var outputStream: OutputStream? = null
    try {
        outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext
        write(outputStream)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.export_success, Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        context.log().e(TAG, "Export failed:\n${e.stackTraceToString()}")
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
        }
    } finally {
        outputStream?.close()
    }
}
