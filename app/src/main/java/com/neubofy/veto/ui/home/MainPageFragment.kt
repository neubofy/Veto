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

        val tvDeviceDetails = view.findViewById<TextView>(R.id.tvDeviceDetails)
        val model = android.os.Build.MODEL
        val battery = com.neubofy.veto.utils.Utils.getBatteryLevel(requireContext())
        val osVersion = android.os.Build.VERSION.RELEASE
        tvDeviceDetails.text = "📱 Model: $model (Android $osVersion)\n🔋 Battery Level: $battery%"

        val locateCmd = com.neubofy.veto.commands.LocateCommand(requireContext())
        val photoCmd = com.neubofy.veto.commands.CameraCommand(requireContext())
        val videoCmd = com.neubofy.veto.commands.VideoCommand(requireContext())
        val audioCmd = com.neubofy.veto.commands.AudioCommand(requireContext())
        val autoLocCmd = com.neubofy.veto.commands.AutoLocCommand(requireContext())
        val theftCmd = com.neubofy.veto.commands.TheftCommand(requireContext())

        val switchCmdLocate = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchCmdLocate)
        val switchCmdPhoto = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchCmdPhoto)
        val switchCmdVideo = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchCmdVideo)
        val switchCmdAudio = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchCmdAudio)

        switchCmdLocate.isChecked = locateCmd.isEnabled()
        switchCmdPhoto.isChecked = photoCmd.isEnabled()
        switchCmdVideo.isChecked = videoCmd.isEnabled()
        switchCmdAudio.isChecked = audioCmd.isEnabled()

        switchCmdLocate.setOnCheckedChangeListener { _, isChecked ->
            locateCmd.setEnabled(isChecked)
            autoLocCmd.setEnabled(isChecked)
            theftCmd.setEnabled(isChecked)
        }
        switchCmdPhoto.setOnCheckedChangeListener { _, isChecked -> photoCmd.setEnabled(isChecked) }
        switchCmdVideo.setOnCheckedChangeListener { _, isChecked -> videoCmd.setEnabled(isChecked) }
        switchCmdAudio.setOnCheckedChangeListener { _, isChecked -> audioCmd.setEnabled(isChecked) }

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
