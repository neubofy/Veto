package com.neubofy.veto.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neubofy.veto.R
import com.neubofy.veto.commands.Command
import com.neubofy.veto.commands.availableCommands
import com.neubofy.veto.utils.CypherUtils

class PasswordSetDialog(
    val context: Context,
    val onSuccess: (String) -> Unit,
) {

    var dialog: AlertDialog

    init {
        val passwordLayout: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_password_set, null)
        val editTextPassword = passwordLayout.findViewById<EditText?>(R.id.editTextPassword)

        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.password_enter)
            .setView(passwordLayout)
            .setPositiveButton(R.string.Ok) { _, _ ->
                val password = editTextPassword.getText().toString()

                if (password.isBlank()) {
                    // an empty password should not trigger the min length check
                    onSuccess("")
                } else if (availableCommands(context).stream()
                        .anyMatch { cmd: Command? -> cmd!!.keyword == password }
                ) {
                    Toast.makeText(
                        context,
                        R.string.password_match_command_keyword,
                        Toast.LENGTH_LONG
                    ).show()
                } else if (password.length < CypherUtils.MIN_PASSWORD_LENGTH) {
                    Toast.makeText(context, R.string.password_min_length, Toast.LENGTH_LONG)
                        .show()
                } else {
                    onSuccess(password)
                }
            }
            .create()
    }

    fun show() {
        dialog.show();
    }
}
