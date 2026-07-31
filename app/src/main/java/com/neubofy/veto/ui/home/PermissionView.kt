package com.neubofy.veto.ui.home

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neubofy.veto.databinding.ItemPermissionBinding
import com.neubofy.veto.permissions.Permission


class PermissionView @JvmOverloads constructor(
    private val context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs), DefaultLifecycleObserver {

    private val binding = ItemPermissionBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var p: Permission
    private lateinit var activity: Activity

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        updateView()
    }

    fun setPermission(p: Permission, activity: AppCompatActivity, hideDescription: Boolean = false) {
        this.p = p
        this.activity = activity
        activity.lifecycle.addObserver(this)
        updateView()
    }

    private fun updateView() {
        if (!this::p.isInitialized) return

        binding.permName.text = context.getString(p.name)

        val descId = p.description

        binding.permInfoIcon.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(p.name)
                .setMessage(descId ?: p.name)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        if (p.isGranted(context)) {
            binding.icCheck.visibility = View.VISIBLE
            binding.buttonGrant.visibility = View.GONE
        } else {
            binding.icCheck.visibility = View.GONE
            binding.buttonGrant.visibility = View.VISIBLE

            binding.buttonGrant.setOnClickListener {
                p.request(activity)
            }
        }
    }
}
