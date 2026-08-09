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
            val simStateExtra = intent.getStringExtra("ss") ?: ""

            val isAbsent = simState == TelephonyManager.SIM_STATE_ABSENT || simStateExtra == "ABSENT"
            val isPinLocked = simState == TelephonyManager.SIM_STATE_PIN_REQUIRED || 
                              simState == TelephonyManager.SIM_STATE_PUK_REQUIRED || 
                              simStateExtra == "PIN_REQUIRED" || 
                              simStateExtra == "PUK_REQUIRED" || 
                              simStateExtra == "LOCKED"
            val isReady = simState == TelephonyManager.SIM_STATE_READY || simStateExtra == "READY" || simStateExtra == "LOADED"

            if (isAbsent) {
                if (settings.get(Settings.SET_AUTO_THEFT_SIM_REMOVED) as Boolean) {
                    AutoTheftManager.triggerSuspectedMode(context, "SIM card removed")
                }
            } else if (isReady || isPinLocked) {
                if (settings.get(Settings.SET_AUTO_THEFT_PROOF_SIM) as Boolean) {
                    val ownerSimString = settings.get(Settings.SET_AUTO_THEFT_OWNER_SIM) as String
                    val ownerSims = ownerSimString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    // Scenario 1: If no specific owner SIM number is configured, any SIM re-insertion cancels suspected mode
                    if (ownerSims.isEmpty()) {
                        AutoTheftManager.cancelSuspectedMode(context)
                        return
                    }

                    // Scenario 2: If SIM PIN Lock is active (PIN_REQUIRED), a physical SIM was inserted into the tray!
                    // Since SIM PIN hides phone numbers from API until unlocked, treat re-insertion as proof of physical SIM tray access
                    if (isPinLocked) {
                        AutoTheftManager.cancelSuspectedMode(context)
                        return
                    }

                    // Scenario 3: Verify SIM details against owner SIM numbers/IDs
                    try {
                        val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                        val activeSubscriptionInfoList = subManager.activeSubscriptionInfoList

                        var isOwnerSim = false
                        activeSubscriptionInfoList?.forEach { subInfo ->
                            var number: String? = null
                            @Suppress("MissingPermission")
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                number = subManager.getPhoneNumber(subInfo.subscriptionId)
                            }
                            if (number.isNullOrBlank()) {
                                @Suppress("DEPRECATION")
                                number = subInfo.number
                            }
                            
                            val iccId = subInfo.iccId
                            val cardId = subInfo.cardId.toString()
                            val subId = subInfo.subscriptionId.toString()

                            if ((number != null && ownerSims.contains(number)) ||
                                (iccId != null && ownerSims.contains(iccId)) ||
                                ownerSims.contains(cardId) ||
                                ownerSims.contains(subId)) {
                                isOwnerSim = true
                            }
                        }

                        val line1 = try { telephonyManager.line1Number } catch (_: Exception) { null }
                        val simSerial = try { @Suppress("DEPRECATION") telephonyManager.simSerialNumber } catch (_: Exception) { null }
                        val simOperator = telephonyManager.simOperator

                        if (ownerSims.contains(line1) || ownerSims.contains(simSerial) || ownerSims.contains(simOperator)) {
                            isOwnerSim = true
                        }

                        if (isOwnerSim) {
                            AutoTheftManager.cancelSuspectedMode(context)
                        } else if (isReady && settings.get(Settings.SET_AUTO_THEFT_SIM_REMOVED) as Boolean) {
                            // Non-owner SIM inserted -> trigger unauthorized SIM warning
                            AutoTheftManager.triggerSuspectedMode(context, "Unauthorized SIM card inserted")
                        }
                    } catch (e: SecurityException) {
                        // Fallback if permission not granted: cancel suspected mode when SIM is physically re-inserted
                        AutoTheftManager.cancelSuspectedMode(context)
                    }
                }
            }
        }
    }
}
