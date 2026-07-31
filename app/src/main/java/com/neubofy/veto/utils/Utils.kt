package com.neubofy.veto.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.neubofy.veto.R

class Utils {

    companion object {
        @JvmStatic
        fun copyToClipboard(context: Context, label: String?, text: String?) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                val copied = context.getString(R.string.copied)
                Toast.makeText(context, copied, Toast.LENGTH_LONG).show()
            }
        }

        @JvmStatic
        fun pasteFromClipboard(context: Context): CharSequence? {
            val clipboardManager =
                context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager
            return clipboardManager.primaryClip?.getItemAt(0)?.text
        }

        @JvmStatic
        fun openUrl(context: Context, url: String?) {
            val uri = Uri.parse(url)
            val i = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(i)
        }

        @JvmStatic
        fun openApp(context: Context, packageName: String) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            context.startActivity(intent)
        }

        @JvmStatic
        fun getGeoURI(lat: Double, lon: Double): String {
            return "geo:$lat,$lon"
        }

        @JvmStatic
        fun getGoogleMapsLink(lat: Double, lon: Double): String {
            return "https://www.google.com/maps/search/?api=1&query=$lat,$lon"
        }

        fun getBatteryLevel(context: Context): Int {
            val manager = context.getSystemService(BatteryManager::class.java)
            return manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }
    }
}
