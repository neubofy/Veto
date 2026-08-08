package com.neubofy.veto.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import com.neubofy.veto.commands.availableCommands
import com.neubofy.veto.ui.TaggedFragment
import com.neubofy.veto.ui.theme.VetoTheme

class CommandListFragment : TaggedFragment() {

    override fun getStaticTag() = "CommandListFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                VetoTheme {
                    CommandListScreen(
                        commands = availableCommands(context),
                        activity = activity as AppCompatActivity
                    )
                }
            }
        }
    }
}
