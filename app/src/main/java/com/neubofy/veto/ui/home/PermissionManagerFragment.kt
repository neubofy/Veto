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

class PermissionManagerFragment : TaggedFragment() {

    companion object {
        const val ARG_HIGHLIGHT_PERMISSION_NAME = "ARG_HIGHLIGHT_PERMISSION_NAME"
    }

    override fun getStaticTag() = "PermissionManagerFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_command_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val highlightName = arguments?.getInt(ARG_HIGHLIGHT_PERMISSION_NAME, -1) ?: -1
        val permissionListAdapter = PermissionListAdapter(activity as AppCompatActivity, highlightName)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_commands)
        recyclerView.adapter = permissionListAdapter

        val perms = com.neubofy.veto.permissions.globalAppPermissions()
        permissionListAdapter.submitList(perms)

        if (highlightName != -1) {
            val targetIndex = perms.indexOfFirst { it.name == highlightName }
            if (targetIndex != -1) {
                recyclerView.post {
                    recyclerView.smoothScrollToPosition(targetIndex)
                }
            }
        }
    }

    class PermissionListAdapter(
        private val activity: AppCompatActivity,
        private val highlightName: Int = -1
    ) : androidx.recyclerview.widget.ListAdapter<com.neubofy.veto.permissions.Permission, PermissionListAdapter.ViewHolder>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<com.neubofy.veto.permissions.Permission>() {
            override fun areItemsTheSame(oldItem: com.neubofy.veto.permissions.Permission, newItem: com.neubofy.veto.permissions.Permission): Boolean = oldItem.name == newItem.name
            override fun areContentsTheSame(oldItem: com.neubofy.veto.permissions.Permission, newItem: com.neubofy.veto.permissions.Permission): Boolean = oldItem.name == newItem.name
        }
    ) {
        inner class ViewHolder(val view: PermissionView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = PermissionView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            holder.view.setPermission(item, activity)
            if (highlightName != -1 && item.name == highlightName) {
                holder.view.postDelayed({ holder.view.highlightCard() }, 300)
            }
        }
    }
}
