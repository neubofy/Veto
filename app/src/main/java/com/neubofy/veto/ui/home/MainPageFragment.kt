package com.neubofy.veto.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        view.findViewById<MaterialCardView>(R.id.card_about).setOnClickListener {
            startActivity(Intent(requireContext(), com.neubofy.veto.ui.settings.AboutActivity::class.java))
        }

        view.findViewById<MaterialCardView>(R.id.card_settings).setOnClickListener {
            startActivity(Intent(requireContext(), com.neubofy.veto.ui.settings.SettingsActivity::class.java))
        }

        view.findViewById<MaterialCardView>(R.id.card_logs).setOnClickListener {
            startActivity(Intent(requireContext(), com.neubofy.veto.ui.settings.LogViewActivity::class.java))
        }

        // Cloud Storage Setup & 1-Click Folder Open
        view.findViewById<View>(R.id.btn_setup_cloud)?.setOnClickListener {
            startActivity(Intent(requireContext(), com.neubofy.veto.ui.settings.AccountActivity::class.java))
        }

        view.findViewById<View>(R.id.btn_open_cloud_folder)?.setOnClickListener {
            try {
                val driveIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://drive.google.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(driveIntent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(requireContext(), "Opening Google Drive web...", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // Local Storage Setup & 1-Click Folder Open
        view.findViewById<View>(R.id.btn_setup_local)?.setOnClickListener {
            val path = com.neubofy.veto.utils.MediaStorageManager.setupStorage(requireContext())
            android.widget.Toast.makeText(requireContext(), "Storage setup verified:\n$path", android.widget.Toast.LENGTH_LONG).show()
        }

        view.findViewById<View>(R.id.btn_open_local_folder)?.setOnClickListener {
            com.neubofy.veto.utils.MediaStorageManager.openLocalFolder(requireContext())
        }

        return view
    }
}
