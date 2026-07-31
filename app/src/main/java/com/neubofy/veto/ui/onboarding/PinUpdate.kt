package com.neubofy.veto.ui.onboarding

import android.content.Context
import com.neubofy.veto.R
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.ui.settings.SettingsActivity
import com.neubofy.veto.utils.Notifications

class PinUpdate {

    companion object {
        fun migratePin(context: Context) {
            val settings = SettingsRepository.getInstance(context)
            val oldPinHash = settings.get(Settings.SET_PIN) as String

            if (oldPinHash.isNotBlank()) {
                val title = context.getString(R.string.notify_crypto_update_title)
                val text = context.getString(R.string.update_pin_hash_change_text)
                Notifications.notify(
                    context,
                    title,
                    text,
                    Notifications.CHANNEL_SECURITY,
                    cls = SettingsActivity::class.java,
                )
            }
            settings.remove(Settings.SET_PIN)
        }
    }
}
