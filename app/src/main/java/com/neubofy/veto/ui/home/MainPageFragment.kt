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



        view.findViewById<MaterialCardView>(R.id.card_allowed_contacts).setOnClickListener {
            startActivity(Intent(requireContext(), AllowlistActivity::class.java))
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

        view.findViewById<MaterialCardView>(R.id.card_website).setOnClickListener {
            startActivity(Intent(requireContext(), AccountActivity::class.java))
        }

        view.findViewById<View>(R.id.card_settings).setOnClickListener {
            startActivity(Intent(activity, com.neubofy.veto.ui.settings.SettingsActivity::class.java))
        }

        return view
    }
}
