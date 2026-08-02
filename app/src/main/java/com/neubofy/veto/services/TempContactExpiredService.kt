package com.neubofy.veto.services

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.neubofy.veto.R
import com.neubofy.veto.data.TemporaryAllowlistRepository
import com.neubofy.veto.transports.SmsTransport
import com.neubofy.veto.utils.log

class TempContactExpiredService : JobService() {

    private val TAG = TempContactExpiredService::class.java.simpleName

    override fun onStartJob(params: JobParameters?): Boolean {
        val repo = TemporaryAllowlistRepository.getInstance(this)
        val expired = repo.removeExpired()

        for ((number, subId) in expired) {
            val msg = getString(R.string.temporary_allowlist_expired)
            val transport = SmsTransport(this, number, subId)
            transport.send(this, msg, null)
            log().i(TAG, "Phone number expired: $number")
        }

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    companion object {
        private const val FIVE_MINS_MILLIS = 5 * 60 * 1000L

        @JvmStatic
        fun scheduleJob(context: Context, initialDelay: Int) {
            val serviceComponent = ComponentName(context, TempContactExpiredService::class.java)
            val jobId = System.currentTimeMillis().toInt()

            val builder = JobInfo.Builder(jobId, serviceComponent)
                .setMinimumLatency(initialDelay.toLong())
                .setOverrideDeadline(initialDelay + FIVE_MINS_MILLIS)

            val jobScheduler = context.getSystemService(JobScheduler::class.java)
            jobScheduler?.schedule(builder.build())
        }
    }
}
