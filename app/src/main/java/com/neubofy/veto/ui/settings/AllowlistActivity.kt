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
import com.neubofy.veto.data.TemporaryAllowlistRepository
import com.neubofy.veto.ui.UiUtil
import com.neubofy.veto.ui.VetoActivity
import com.neubofy.veto.ui.allowlist.AllowlistAdapter
import com.neubofy.veto.ui.allowlist.AllowlistItem

class AllowlistActivity : VetoActivity() {

    private lateinit var allowlistRepository: AllowlistRepository
    private lateinit var temporaryAllowlistRepository: TemporaryAllowlistRepository
    private lateinit var settings: SettingsRepository
    private lateinit var allowlistAdapter: AllowlistAdapter
    private lateinit var temporaryAllowlistAdapter: AllowlistAdapter
    private lateinit var textWhitelistEmpty: TextView
    private lateinit var textTempWhitelistEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allowlist)

        UiUtil.setupEdgeToEdge(findViewById(android.R.id.content))

        allowlistRepository = AllowlistRepository.getInstance(this)
        temporaryAllowlistRepository = TemporaryAllowlistRepository.getInstance(this)
        settings = SettingsRepository.getInstance(this)

        allowlistAdapter = AllowlistAdapter(
            onDeleteClicked = { phoneNumber -> onDeleteContact(phoneNumber) },
            onStarClicked = { phoneNumber -> onStarContact(phoneNumber) },
            onEditClicked = { phoneNumber -> onEditContact(phoneNumber) }
        )
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_allowlist)
        recyclerView.adapter = allowlistAdapter

        temporaryAllowlistAdapter = AllowlistAdapter(
            onDeleteClicked = { phoneNumber -> onDeleteTempContact(phoneNumber) },
            onStarClicked = { _ -> 
                Toast.makeText(this, "Cannot star a temporary contact.", Toast.LENGTH_SHORT).show()
            },
            onEditClicked = { _ -> 
                Toast.makeText(this, "Cannot edit a temporary contact.", Toast.LENGTH_SHORT).show()
            }
        )
        val tempRecyclerView = findViewById<RecyclerView>(R.id.recycler_temporary_allowlist)
        tempRecyclerView.adapter = temporaryAllowlistAdapter

        textWhitelistEmpty = findViewById(R.id.whitelistEmpty)
        textTempWhitelistEmpty = findViewById(R.id.tempWhitelistEmpty)
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

        temporaryAllowlistRepository.removeExpired()
        val tempList = temporaryAllowlistRepository.getList()
        if (tempList.isEmpty()) {
            textTempWhitelistEmpty.visibility = View.VISIBLE
        } else {
            textTempWhitelistEmpty.visibility = View.GONE
        }
        
        val tempItems = tempList.map { temp ->
            val remainingMillis = temp.createdTimeMillis + com.neubofy.veto.data.TEMP_USAGE_VALIDITY_MILLIS - System.currentTimeMillis()
            val remainingMinutes = Math.max(1, remainingMillis / (60 * 1000))
            AllowlistItem("Temporary (${remainingMinutes}m)", temp.number, false)
        }
        temporaryAllowlistAdapter.submitList(tempItems)
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

    private fun onEditContact(phoneNumber: String) {
        val contact = allowlistRepository.list.find { android.telephony.PhoneNumberUtils.compare(it.number, phoneNumber) } ?: return
        
        val layout = layoutInflater.inflate(R.layout.dialog_phone_number, null)
        val nameInput = layout.findViewById<EditText>(R.id.editTextName)
        val phoneNumberInput = layout.findViewById<EditText>(R.id.editTextPhoneNumber)
        
        nameInput.setText(contact.name)
        phoneNumberInput.setText(contact.number)

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Contact")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString()
                val number = phoneNumberInput.text.toString()
                allowlistRepository.editContact(contact.number, name, number)
                updateScreen()
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

    private fun onDeleteContact(phoneNumber: String) {
        allowlistRepository.remove(phoneNumber)
        updateScreen()
    }
    
    private fun onDeleteTempContact(phoneNumber: String) {
        temporaryAllowlistRepository.remove(phoneNumber)
        updateScreen()
    }

    private fun onStarContact(phoneNumber: String) {
        allowlistRepository.starContact(phoneNumber)
        updateScreen()
    }
}
