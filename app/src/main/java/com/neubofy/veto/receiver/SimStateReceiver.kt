package com.neubofy.veto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.utils.AutoTheftManager

class SimStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.SIM_STATE_CHANGED") {
            val settings = SettingsRepository.getInstance(context)
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val simState = telephonyManager.simState

            if (simState == TelephonyManager.SIM_STATE_ABSENT) {
                if (settings.get(Settings.SET_AUTO_THEFT_SIM_REMOVED) as Boolean) {
                    AutoTheftManager.triggerSuspectedMode(context, "SIM card removed")
                }
            } else if (simState == TelephonyManager.SIM_STATE_READY) {
                if (settings.get(Settings.SET_AUTO_THEFT_PROOF_SIM) as Boolean) {
                    // Check if the loaded SIM matches the owner SIM
                    val ownerSimString = settings.get(Settings.SET_AUTO_THEFT_OWNER_SIM) as String
                    val ownerSims = ownerSimString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    if (ownerSims.isEmpty()) {
                        // If no owner SIM is setup, any SIM re-insertion cancels it
                        AutoTheftManager.cancelSuspectedMode(context)
                        return
                    }

                    try {
                        val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                        val activeSubscriptionInfoList = subManager.activeSubscriptionInfoList

                        var isOwnerSim = false
                        activeSubscriptionInfoList?.forEach { subInfo ->
                            @Suppress("DEPRECATION")
                            val number = subInfo.number
                            if (number != null && ownerSims.contains(number)) {
                                isOwnerSim = true
                            }
                        }

                        if (isOwnerSim) {
                            AutoTheftManager.cancelSuspectedMode(context)
                        } else if (settings.get(Settings.SET_AUTO_THEFT_SIM_REMOVED) as Boolean) {
                            // Non-owner SIM inserted -> trigger again just in case
                            AutoTheftManager.triggerSuspectedMode(context, "Unauthorized SIM card inserted")
                        }
                    } catch (e: SecurityException) {
                        // Fallback if no permission
                        AutoTheftManager.cancelSuspectedMode(context)
                    }
                }
            }
        }
    }
}
