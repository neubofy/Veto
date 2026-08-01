package com.neubofy.veto.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.neubofy.veto.R
import com.neubofy.veto.ui.TaggedFragment
import com.neubofy.veto.ui.settings.AllowlistActivity
import com.neubofy.veto.ui.settings.AccountActivity

class MainPageFragment : TaggedFragment() {

    override fun getStaticTag() = "MainPageFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main_page, container, false)

        val ctx = requireContext()
        val tvDeviceDetails = view.findViewById<TextView>(R.id.tvDeviceDetails)
        
        // 1. Model & OS
        val model = android.os.Build.MODEL
        val osVersion = android.os.Build.VERSION.RELEASE

        // 2. Battery & Charging
        val batteryLevel = com.neubofy.veto.utils.Utils.getBatteryLevel(ctx)
        val batteryIntent = ctx.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
        val batteryStr = "$batteryLevel% ${if (isCharging) "⚡ (Charging)" else ""}"

        // 3. Bluetooth Status (Safe Permission Handling)
        var btStr = "Unavailable"
        try {
            val hasBtPerm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.BLUETOOTH) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }

            if (hasBtPerm) {
                val btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                btStr = if (btAdapter != null && btAdapter.isEnabled) "🔷 Enabled" else "⚪ Disabled"
            } else {
                btStr = "⚠️ Permission Required"
            }
        } catch (e: Exception) {
            btStr = "Unavailable"
        }

        // 4. DND Mode
        var dndStr = "Off"
        try {
            val nm = ctx.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            dndStr = when (nm.currentInterruptionFilter) {
                android.app.NotificationManager.INTERRUPTION_FILTER_ALL -> "Off (Allow All)"
                android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "Priority Only"
                android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS -> "Alarms Only"
                android.app.NotificationManager.INTERRUPTION_FILTER_NONE -> "Total Silence"
                else -> "Off"
            }
        } catch (e: Exception) {
            dndStr = "Unavailable"
        }

        // 5. Sound / Ringer Mode
        var soundStr = "Normal"
        try {
            val am = ctx.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            soundStr = when (am.ringerMode) {
                android.media.AudioManager.RINGER_MODE_SILENT -> "🔇 Silent"
                android.media.AudioManager.RINGER_MODE_VIBRATE -> "📳 Vibrate"
                android.media.AudioManager.RINGER_MODE_NORMAL -> "🔊 Normal"
                else -> "Normal"
            }
        } catch (e: Exception) {
            soundStr = "Unavailable"
        }

        // 6. Flashlight
        val hasFlash = ctx.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FLASH)
        val flashStr = if (hasFlash) "🔦 Available" else "N/A"

        tvDeviceDetails.text = "📱 Model: $model (Android $osVersion)\n🔋 Battery: $batteryStr\n🔷 Bluetooth: $btStr\n🌙 DND Mode: $dndStr\n🔊 Sound Mode: $soundStr\n🔦 Flashlight: $flashStr"

        view.findViewById<MaterialCardView>(R.id.card_commands).setOnClickListener {
            startActivity(Intent(requireContext(), com.neubofy.veto.ui.settings.CommandsActivity::class.java))
        }





        view.findViewById<MaterialCardView>(R.id.card_transport_channels).setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TransportListFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<MaterialCardView>(R.id.card_permission_manager).setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PermissionManagerFragment())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.card_settings).setOnClickListener {
            startActivity(Intent(activity, com.neubofy.veto.ui.settings.SettingsActivity::class.java))
        }

        return view
    }
}
