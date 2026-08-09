package com.neubofy.veto.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neubofy.veto.R
import com.neubofy.veto.data.AllowlistRepository
import com.neubofy.veto.data.Contact
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.UiUtil
import com.neubofy.veto.ui.VetoActivity
import com.neubofy.veto.ui.allowlist.AllowlistAdapter

class AllowlistActivity : VetoActivity() {

    private lateinit var allowlistRepository: AllowlistRepository
    private lateinit var settings: SettingsRepository
    private lateinit var allowlistAdapter: AllowlistAdapter
    private lateinit var textWhitelistEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allowlist)

        UiUtil.setupEdgeToEdge(findViewById(android.R.id.content))

        allowlistRepository = AllowlistRepository.getInstance(this)
        settings = SettingsRepository.getInstance(this)

        allowlistAdapter = AllowlistAdapter(
            onDeleteClicked = { phoneNumber -> onDeleteContact(phoneNumber) },
            onStarClicked = { phoneNumber -> onToggleStarContact(phoneNumber) }
        )
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_allowlist)
        recyclerView.adapter = allowlistAdapter

        textWhitelistEmpty = findViewById(R.id.whitelistEmpty)
        findViewById<View>(R.id.buttonAddPhoneNumber).setOnClickListener { v -> onAddPhoneNumberClicked(v) }

        updateScreen()
    }

    private fun updateScreen() {
        if (allowlistRepository.list.isEmpty()) {
            textWhitelistEmpty.visibility = View.VISIBLE
        } else {
            textWhitelistEmpty.visibility = View.GONE
        }

        allowlistAdapter.submitContactList(allowlistRepository.list)
    }

    private fun onAddPhoneNumberClicked(v: View) {
        val context = v.context
        val layout = layoutInflater.inflate(R.layout.dialog_phone_number, null)
        val nameInput = layout.findViewById<EditText>(R.id.editTextName)
        val phoneNumberInput = layout.findViewById<EditText>(R.id.editTextPhoneNumber)

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.allowlist_add_phone_number))
            .setView(layout)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val name = nameInput.text.toString()
                val number = phoneNumberInput.text.toString()
                val dummyContact = Contact.from(context, name, number)
                addContactToAllowList(dummyContact)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun addContactToAllowList(contact: Contact?) {
        if (contact == null) {
            Toast.makeText(this, R.string.allowlist_invalid_number, Toast.LENGTH_LONG).show()
        } else {
            if (!allowlistRepository.contains(contact)) {
                allowlistRepository.add(contact)
                updateScreen()

                if (settings.get(Settings.SET_FIRST_TIME_CONTACT_ADDED) as Boolean == false) {
                    val keyword = settings.get(Settings.SET_Veto_COMMAND) as String
                    val message = getString(R.string.tip_first_contact_added, keyword, keyword, keyword)
                    MaterialAlertDialogBuilder(this)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            settings.set(Settings.SET_FIRST_TIME_CONTACT_ADDED, true)
                        }
                        .show()
                }
            } else {
                Toast.makeText(this, R.string.Toast_Duplicate_contact, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onToggleStarContact(phoneNumber: String) {
        allowlistRepository.toggleStarred(phoneNumber)
        updateScreen()
    }

    private fun onDeleteContact(phoneNumber: String) {
        allowlistRepository.remove(phoneNumber)
        updateScreen()
    }
}
