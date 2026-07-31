package com.neubofy.veto.ui.helper;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;
import java.util.List;

import com.neubofy.veto.R;


public class SettingsEntry {
    String string;
    Drawable icon;

    SettingsEntry(Context context, @StringRes int stringId, @DrawableRes int iconId) {
        this.string = context.getString(stringId);
        this.icon = AppCompatResources.getDrawable(context, iconId);
    }

    public static List<SettingsEntry> getSettingsEntries(Context context) {
        List<SettingsEntry> entries = new ArrayList<>();
        entries.add(new SettingsEntry(context, R.string.Settings_LCLDConfig, R.drawable.ic_settings));
        entries.add(new SettingsEntry(context, R.string.Settings_LCLDServer_AddAccount, R.drawable.ic_cloud));
        entries.add(new SettingsEntry(context, R.string.Settings_Appearance, R.drawable.ic_palette));
        entries.add(new SettingsEntry(context, R.string.Settings_Export, R.drawable.ic_import_export));
        entries.add(new SettingsEntry(context, R.string.Settings_Import, R.drawable.ic_import_export));
        entries.add(new SettingsEntry(context, R.string.Settings_Logs, R.drawable.ic_logs));
        entries.add(new SettingsEntry(context, R.string.Settings_About, R.drawable.ic_info));
        return entries;
    }
}
