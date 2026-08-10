package com.neubofy.veto.ui.helper

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import com.neubofy.veto.R

data class SettingsEntry(
    val string: String,
    val icon: Drawable?
) {
    companion object {
        @JvmStatic
        fun getSettingsEntries(context: Context): List<SettingsEntry> {
            return listOf(
                SettingsEntry(context.getString(R.string.Settings_VetoConfig), AppCompatResources.getDrawable(context, R.drawable.ic_settings)),
                SettingsEntry(context.getString(R.string.Settings_WebDashboard), AppCompatResources.getDrawable(context, R.drawable.ic_cloud)),
                SettingsEntry(context.getString(R.string.Settings_Appearance), AppCompatResources.getDrawable(context, R.drawable.ic_palette)),
                SettingsEntry(context.getString(R.string.Settings_Export), AppCompatResources.getDrawable(context, R.drawable.ic_import_export)),
                SettingsEntry(context.getString(R.string.Settings_Import), AppCompatResources.getDrawable(context, R.drawable.ic_import_export)),
                SettingsEntry(context.getString(R.string.Settings_Logs), AppCompatResources.getDrawable(context, R.drawable.ic_logs)),
                SettingsEntry(context.getString(R.string.Settings_About), AppCompatResources.getDrawable(context, R.drawable.ic_info)),
            )
        }
    }
}
