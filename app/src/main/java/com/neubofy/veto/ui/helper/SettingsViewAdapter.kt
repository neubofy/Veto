package com.neubofy.veto.ui.helper

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.neubofy.veto.R

class SettingsViewAdapter(
    context: Context,
    private val settingsEntries: List<SettingsEntry>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = settingsEntries.size

    override fun getItem(position: Int): Any = settingsEntries[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: inflater.inflate(R.layout.item_settings, parent, false)
        val entry = settingsEntries[position]
        
        val name = view.findViewById<TextView>(R.id.textViewSettingsTitle)
        name.text = entry.string
        
        val icon = view.findViewById<ImageView>(R.id.imageViewSettingsIcon)
        icon.setImageDrawable(entry.icon)
        
        return view
    }
}
