package com.neubofy.veto.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.neubofy.veto.utils.log

class VetoFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private val TAG = VetoFirebaseMessagingService::class.java.simpleName
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        log().i(TAG, "New Firebase token received: $token")
        
        // Save to Firestore via Next.js Dashboard API if paired
        com.neubofy.veto.utils.DashboardSync.uploadTokenIfPaired(this)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        log().i(TAG, "Received FCM message from: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            log().i(TAG, "Message data payload: ${remoteMessage.data}")
            
            val commandStr = remoteMessage.data["command"]
            val incomingUid = remoteMessage.data["uid"]
            
            if (commandStr != null) {
                val settings = com.neubofy.veto.data.SettingsRepository.getInstance(this)
                val localUid = settings.get(com.neubofy.veto.data.Settings.SET_VetoSERVER_ID) as String
                
                // Security Verification Layer
                if (incomingUid == null || incomingUid != localUid) {
                    log().e(TAG, "Unverified FCM command received. Mismatched UID. Ignored.")
                    return
                }

                // Prepend trigger word so the parser accepts it
                var triggerWord = settings.get(com.neubofy.veto.data.Settings.SET_Veto_COMMAND) as String
                if (triggerWord.isBlank()) {
                    triggerWord = "veto"
                    settings.set(com.neubofy.veto.data.Settings.SET_Veto_COMMAND, "veto")
                }
                val fullCommand = "$triggerWord $commandStr"

                log().i(TAG, "Enqueuing FCM command execution: $fullCommand")

                val inputData = androidx.work.workDataOf(
                    com.neubofy.veto.workers.CommandExecutionWorker.KEY_COMMAND to fullCommand,
                    com.neubofy.veto.workers.CommandExecutionWorker.KEY_TRANSPORT_TYPE to com.neubofy.veto.workers.CommandExecutionWorker.TRANS_NEXTJS_SERVER,
                    com.neubofy.veto.workers.CommandExecutionWorker.KEY_DESTINATION to "FCM Server",
                )
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<com.neubofy.veto.workers.CommandExecutionWorker>()
                    .setInputData(inputData)
                    .build()
                androidx.work.WorkManager.getInstance(this).enqueue(workRequest)
            } else {
                log().w(TAG, "FCM data payload did not contain a 'command' key")
            }
        }
    }
}
