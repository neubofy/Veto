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

        // Live Terminal Log Preview & Expand Action
        val btnExpand = view.findViewById<View>(R.id.btn_expand_logs)
        val tvTerminalPreview = view.findViewById<android.widget.TextView>(R.id.tv_terminal_preview)

        btnExpand?.setOnClickListener {
            startActivity(Intent(requireContext(), com.neubofy.veto.ui.settings.LogViewActivity::class.java))
        }

        try {
            val logRepo = com.neubofy.veto.data.LogRepository.getInstance(requireContext())
            val recentLogs = synchronized(logRepo.list) {
                logRepo.list.takeLast(4).joinToString("\n") { entry ->
                    "[${entry.tag}] ${entry.msg}"
                }
            }
            tvTerminalPreview?.text = if (recentLogs.isNotBlank()) recentLogs else "[SYSTEM] Veto active and monitoring transports...\n[STATUS] All permissions & services ready."
        } catch (_: Exception) {
            tvTerminalPreview?.text = "[SYSTEM] Veto running cleanly."
        }

        return view
    }
}
