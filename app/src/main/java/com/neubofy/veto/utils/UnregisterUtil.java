package com.neubofy.veto.utils;

import android.content.Context;
import android.content.DialogInterface;

import com.android.volley.VolleyError;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.neubofy.veto.R;
import com.neubofy.veto.data.Settings;
import com.neubofy.veto.data.SettingsRepository;

public class UnregisterUtil {

    public static void showUnregisterFailedDialog(Context context, VolleyError error, OnContinueClickedListener onContinueClickedListener) {

        String message = context.getString(R.string.server_unregister_failed_body);
        if (error != null && error.getMessage() != null) {
            message = message.replace("{ERROR}", error.getMessage());
        } else {
            message = message.replace("{ERROR}", "error or getMessage() was null!");
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.server_unregister_failed_title))
                .setMessage(message)
                .setPositiveButton(context.getString(R.string.server_unregister_continue_anyway),
                        (DialogInterface dialog, int which) -> {
                            SettingsRepository settings = SettingsRepository.Companion.getInstance(context);
                            settings.set(Settings.SET_VetoSERVER_ID, ""); // force local logout
                            onContinueClickedListener.onContinueClicked();
                            dialog.dismiss();
                        })
                .setNegativeButton(context.getString(R.string.server_unregister_dont_continue), (DialogInterface dialog, int which) -> dialog.dismiss())
                .show();
    }

    public interface OnContinueClickedListener {
        void onContinueClicked();
    }

}
