package com.neubofy.veto.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neubofy.veto.R
import com.neubofy.veto.commands.Command
import com.neubofy.veto.commands.CommandHandler
import com.neubofy.veto.commands.availableCommands
import com.neubofy.veto.transports.InAppTransport
import com.neubofy.veto.ui.VetoActivity
import com.neubofy.veto.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import kotlinx.coroutines.launch

class CommandsActivity : VetoActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_commands)

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar))

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_commands_test)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        val commands = availableCommands(this)
        recyclerView.adapter = CommandsAdapter(commands)
    }

    inner class CommandsAdapter(private val commands: List<com.neubofy.veto.commands.Command>) :
        RecyclerView.Adapter<CommandsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.command_name)
            val keyword: TextView = view.findViewById(R.id.command_keyword)
            val testButton: Button = view.findViewById(R.id.button_test)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_command_test, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val cmd = commands[position]
            holder.name.text = cmd.keyword.replaceFirstChar { it.uppercase() }
            holder.keyword.text = "Keyword: ${cmd.keyword}"

            // Disable TEST for sensitive commands
            if (cmd.keyword == "delete" || cmd.keyword == "wipe") {
                holder.testButton.visibility = View.GONE
            } else {
                holder.testButton.visibility = View.VISIBLE
                holder.testButton.setOnClickListener {
                    val transport = InAppTransport(this@CommandsActivity)
                    lifecycleScope.launch {
                        cmd.execute(emptyList(), transport)
                    }
                }
            }
        }

        override fun getItemCount() = commands.size
    }
}
