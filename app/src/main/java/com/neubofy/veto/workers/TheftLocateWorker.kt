package com.neubofy.veto.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neubofy.veto.commands.LocateCommand
import com.neubofy.veto.transports.InAppTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TheftLocateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val locateCmd = LocateCommand(context)
            val dummyTransport = InAppTransport(context)
            locateCmd.executeInternal(emptyList(), dummyTransport)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
