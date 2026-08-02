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
    val minLength: Int = CypherUtils.MIN_PASSWORD_LENGTH,
    val title: String? = null,
    val positiveButtonText: String? = null,
    val message: String? = null,
    val onSuccess: (String) -> Unit,
) {

    var dialog: AlertDialog

    init {
        val passwordLayout: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_password_set, null)
        val editTextPassword = passwordLayout.findViewById<EditText>(R.id.editTextPassword)

        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(title ?: context.getString(R.string.password_enter))
            .setView(passwordLayout)
            .setPositiveButton(positiveButtonText ?: context.getString(R.string.Ok)) { _, _ ->
                val password = editTextPassword.text.toString()

                if (password.isBlank()) {
                    onSuccess("")
                } else if (availableCommands(context).any { cmd: Command -> cmd.keyword == password }) {
                    Toast.makeText(
                        context,
                        R.string.password_match_command_keyword,
                        Toast.LENGTH_LONG
                    ).show()
                } else if (password.length < minLength) {
                    Toast.makeText(context, R.string.password_min_length, Toast.LENGTH_LONG).show()
                } else {
                    onSuccess(password)
                }
            }
            .setNegativeButton(R.string.cancel, null)

        if (!message.isNullOrEmpty()) {
            builder.setMessage(message)
        }

        dialog = builder.create()
    }

    fun show() {
        dialog.show()
    }

    companion object {
        @JvmStatic
        fun showPasswordSetDialog(
            context: Context,
            title: String,
            positiveButtonText: String,
            message: String,
            minLength: Int,
            onSuccess: (String) -> Unit
        ) {
            PasswordSetDialog(
                context = context,
                minLength = minLength,
                title = title,
                positiveButtonText = positiveButtonText,
                message = message,
                onSuccess = onSuccess
            ).show()
        }
    }
}
