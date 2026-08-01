package com.neubofy.veto.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neubofy.veto.locationproviders.AutoLocProvider
import com.neubofy.veto.utils.log

/**
 * A dedicated lightweight WorkManager worker that directly calls AutoLocProvider.
 * This avoids going through the full command parser/handler pipeline for background GPS uploads.
 * Triggered by AutoLocCommand (on/off toggle) via WorkManager PeriodicWorkRequest.
 */
class AutoLocWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    companion object {
        val TAG = AutoLocWorker::class.simpleName
    }

    override suspend fun doWork(): Result {
        applicationContext.log().i(TAG, "AutoLocWorker running background GPS fetch")
        try {
            AutoLocProvider(applicationContext).executeAutoLoc().await()
        } catch (e: Exception) {
            applicationContext.log().e(TAG, "AutoLocWorker failed: ${e.message}")
        }
        return Result.success()
    }
}
