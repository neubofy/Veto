package com.neubofy.veto.locationproviders

import android.content.Context
import android.location.LocationManager
import android.os.Build
import com.neubofy.veto.permissions.WriteSecureSettingsPermission
import com.neubofy.veto.utils.SecureSettings
import com.neubofy.veto.utils.SingletonHolder
import com.neubofy.veto.utils.log


data class AddJobResult(
    val jobId: Long,
    val isLocationOn: Boolean,
)

class LocationAutoOnOffHandler private constructor(val context: Context) {

    companion object :
        SingletonHolder<LocationAutoOnOffHandler, Context>(::LocationAutoOnOffHandler) {

        private val TAG = LocationAutoOnOffHandler::class.simpleName
    }

    private var isTurnedOnByUs = false

    /**
     * Set of jobs that are currently running that need the location to be on.
     */
    private val runningJobs = mutableSetOf<Long>()

    // Synchronise adding and removing jobs. This avoids race conditions between concurrent jobs
    // which could result in broken state.
    private val lock = Any()

    /**
     * Turns the location on for a job.
     *
     * @return
     * - A jobId that should be passed to removeJob()
     * - True if the location is on, false otherwise.
     */
    fun addJob(): AddJobResult = synchronized(lock) {
        val jobId = System.currentTimeMillis()

        if (isLocationOn(context)) {
            if (isTurnedOnByUs) {
                // add this job, so that any job that exits earlier does not turn it off
                runningJobs.add(jobId)
                context.log().d(TAG, "Adding job=$jobId")
            }
            return AddJobResult(jobId, true)
        }

        if (!WriteSecureSettingsPermission().isGranted(context)) {
            return AddJobResult(jobId, false)
        }

        SecureSettings.turnGPS(context, true)
        isTurnedOnByUs = true
        runningJobs.add(jobId)
        context.log().d(TAG, "Adding job=$jobId")

        // Give it some time to turn on
        Thread.sleep(500)

        return AddJobResult(jobId, true)
    }

    fun removeJob(jobId: Long) = synchronized(lock) {
        runningJobs.remove(jobId)
        cleanJobs()
        context.log().d(TAG, "Removing job=$jobId remaining=$runningJobs")

        if (runningJobs.isEmpty() && isTurnedOnByUs) {
            SecureSettings.turnGPS(context, false)
            isTurnedOnByUs = false
        }
    }

    // Remove outdated jobs that never removed themselves
    private fun cleanJobs() = synchronized(lock) {
        val now = System.currentTimeMillis()
        val old = runningJobs.toSet() // clone to avoid modifying the set that we are iteration over

        for (id in old) {
            if (id + MAX_GPS_DURATION_MILLIS < now) {
                runningJobs.remove(id)
            }
        }
    }
}


fun isLocationOn(context: Context): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        lm.isLocationEnabled
    } else lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}
