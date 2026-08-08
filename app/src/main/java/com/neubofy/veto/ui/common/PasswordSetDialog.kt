package com.neubofy.veto.ui.common

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neubofy.veto.R
import com.neubofy.veto.commands.Command
import com.neubofy.veto.commands.availableCommands
import com.neubofy.veto.ui.theme.VetoTheme
import com.neubofy.veto.utils.CypherUtils

class PasswordSetDialog(
    val context: Context,
    val minLength: Int = CypherUtils.MIN_PASSWORD_LENGTH,
    val title: String? = null,
    val positiveButtonText: String? = null,
    val message: String? = null,
    val onSuccess: (String) -> Unit,
) {

    var dialog: AlertDialog? = null
    var composeView: ComposeView? = null

    init {
        // Since we are migrating an imperative class to compose, we will bridge it by showing the compose
        // dialog directly within the parent's compose tree if possible, or by inflating a ComposeView
        // in the legacy MaterialAlertDialogBuilder if we must remain compatible with imperative calls.
        // For minimal breakage of existing calling code (e.g. SettingsActivity), we use ComposeView inside the legacy dialog.

        composeView = ComposeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setContent {
                VetoTheme {
                    PasswordSetDialogCompose(
                        title = title ?: context.getString(R.string.password_enter),
                        message = message,
                        positiveButtonText = positiveButtonText ?: context.getString(R.string.Ok),
                        minLength = minLength,
                        onDismiss = { dialog?.dismiss() },
                        onConfirm = { password ->
                            if (password.isBlank()) {
                                onSuccess("")
                                dialog?.dismiss()
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
                                dialog?.dismiss()
                            }
                        }
                    )
                }
            }
        }

        val builder = MaterialAlertDialogBuilder(context)
            .setView(composeView)

        // We remove the default buttons because they are now handled by the Compose AlertDialog
        dialog = builder.create()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    fun show() {
        dialog?.show()
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
