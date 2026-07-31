package com.neubofy.veto.ui.helper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import com.neubofy.veto.R;

public class SettingsViewAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<SettingsEntry> settingsEntries;

    public SettingsViewAdapter(Context context, List<SettingsEntry> settingsEntries) {
        inflater = (LayoutInflater.from(context));
        this.settingsEntries = settingsEntries;
    }

    @Override
    public int getCount() {
        return settingsEntries.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        view = inflater.inflate(R.layout.item_settings, null);
        SettingsEntry entry = settingsEntries.get(position);
        TextView name = view.findViewById(R.id.textViewSettingsTitle);
        name.setText(entry.string);
        ImageView icon = view.findViewById(R.id.imageViewSettingsIcon);
        icon.setImageDrawable(entry.icon);
        return view;
    }
}
