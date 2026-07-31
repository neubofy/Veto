package com.neubofy.veto.ui.settings;

import static com.neubofy.veto.ui.UiUtil.setupEdgeToEdge;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.neubofy.veto.R;
import com.neubofy.veto.data.AllowlistRepository;
import com.neubofy.veto.data.Contact;
import com.neubofy.veto.data.Settings;
import com.neubofy.veto.data.SettingsRepository;
import com.neubofy.veto.ui.VetoActivity;
import com.neubofy.veto.ui.allowlist.AllowlistAdapter;
import kotlin.Unit;


public class AllowlistActivity extends VetoActivity {

    private AllowlistRepository allowlistRepository;
    private SettingsRepository settings;

    private AllowlistAdapter allowlistAdapter;

    private TextView textWhitelistEmpty;

    private static final int REQUEST_CODE = 6438;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allowlist);

        setupEdgeToEdge(findViewById(android.R.id.content));

        allowlistRepository = AllowlistRepository.Companion.getInstance(this);
        settings = SettingsRepository.Companion.getInstance(this);

        allowlistAdapter = new AllowlistAdapter(this::onDeleteContact);
        RecyclerView recyclerView = findViewById(R.id.recycler_allowlist);
        recyclerView.setAdapter(allowlistAdapter);

        textWhitelistEmpty = findViewById(R.id.whitelistEmpty);
        findViewById(R.id.buttonAddPhoneNumber).setOnClickListener(this::onAddPhoneNumberClicked);

        updateScreen();
    }

    private void updateScreen() {
        if (allowlistRepository.getList().isEmpty()) {
            textWhitelistEmpty.setVisibility(View.VISIBLE);
        } else {
            textWhitelistEmpty.setVisibility(View.GONE);
        }

        allowlistAdapter.submitContactList(allowlistRepository.getList());
    }

    private void onAddPhoneNumberClicked(View v) {
        Context context = v.getContext();
        View layout = getLayoutInflater().inflate(R.layout.dialog_phone_number, null);
        EditText nameInput = layout.findViewById(R.id.editTextName);
        EditText phoneNumberInput = layout.findViewById(R.id.editTextPhoneNumber);

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.allowlist_add_phone_number))
                .setView(layout)
                .setPositiveButton(getString(R.string.add), (dialog, whichButton) -> {
                    String name = nameInput.getText().toString();
                    String number = phoneNumberInput.getText().toString();
                    Contact dummyContact = Contact.from(context, name, number);
                    addContactToAllowList(dummyContact);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void addContactToAllowList(@Nullable Contact contact) {
        if (contact == null) {
            Toast.makeText(this, R.string.allowlist_invalid_number, Toast.LENGTH_LONG).show();
        } else {
            if (!allowlistRepository.contains(contact)) {
                allowlistRepository.add(contact);
                updateScreen();

                if (!(Boolean) settings.get(Settings.SET_FIRST_TIME_CONTACT_ADDED)) {
                    String keyword = (String) settings.get(Settings.SET_Veto_COMMAND);
                    String message = getString(R.string.tip_first_contact_added, keyword, keyword, keyword);
                    new MaterialAlertDialogBuilder(this)
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    settings.set(Settings.SET_FIRST_TIME_CONTACT_ADDED, true);
                                }
                            })
                            .show();
                }
            } else {
                Toast.makeText(this, R.string.Toast_Duplicate_contact, Toast.LENGTH_LONG).show();
            }
        }
    }

    private Unit onDeleteContact(String phoneNumber) {
        allowlistRepository.remove(phoneNumber);
        updateScreen();
        // make Kotlin-interop happy
        return null;
    }

}