package com.neubofy.veto.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.neubofy.veto.R
import com.neubofy.veto.commands.availableCommands
import com.neubofy.veto.ui.TaggedFragment


class CommandListFragment : TaggedFragment() {

    override fun getStaticTag() = "CommandListFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_command_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val commandListAdapter = CommandListAdapter(activity as AppCompatActivity)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_commands)
        recyclerView.adapter = commandListAdapter

        commandListAdapter.submitList(availableCommands(view.context))
    }
}
