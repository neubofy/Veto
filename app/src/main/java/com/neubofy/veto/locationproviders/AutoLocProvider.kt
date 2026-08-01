package com.neubofy.veto.locationproviders

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.data.VetoLocation
import com.neubofy.veto.transports.NextJsServerTransport
import com.neubofy.veto.utils.log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AutoLocProvider(private val context: Context) : LocationListener {

    companion object {
        private val TAG = AutoLocProvider::class.simpleName
        private const val MAX_WAIT_MILLIS = 15_000L
    }

    private val locationManager = context.getSystemService(LocationManager::class.java)
    private var isFinished = false
    private var deferred: CompletableDeferred<Unit>? = null

    @SuppressLint("MissingPermission")
    suspend fun executeAutoLoc(): CompletableDeferred<Unit> {
        val def = CompletableDeferred<Unit>()
        deferred = def

        if (locationManager == null || !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            context.log().d(TAG, "GPS provider disabled. Cannot run AutoLoc.")
            def.complete(Unit)
            return def
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                this,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            context.log().e(TAG, "Failed to request location updates for AutoLoc: ${e.message}")
            def.complete(Unit)
            return def
        }

        // Timeout guard to prevent hanging coroutines
        CoroutineScope(Dispatchers.IO + Job()).launch {
            delay(MAX_WAIT_MILLIS)
            if (!isFinished) {
                context.log().d(TAG, "AutoLoc timeout reached. Using last known location if available.")
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnown != null) {
                    processLocation(lastKnown)
                } else {
                    cleanup()
                }
            }
        }

        return def
    }

    override fun onLocationChanged(location: Location) {
        if (isFinished) return
        processLocation(location)
    }

    private fun processLocation(location: Location) {
        if (isFinished) return
        isFinished = true

        try {
            val vetoLocation = VetoLocation.fromAndroidLocation(context, location)
            val settings = SettingsRepository.getInstance(context)
            
            // Always store in local 5-item cached history
            settings.storeRecentLocation(vetoLocation)

            // Calculate distance threshold (> 100 meters)
            val lastUploaded = settings.getLastUploadedLocation()
            var shouldUpload = false

            if (lastUploaded == null) {
                shouldUpload = true
            } else {
                val results = FloatArray(1)
                Location.distanceBetween(
                    lastUploaded.lat, lastUploaded.lon,
                    vetoLocation.lat, vetoLocation.lon,
                    results
                )
                if (results[0] > 100f) {
                    shouldUpload = true
                }
            }

            if (shouldUpload) {
                settings.setLastUploadedLocation(vetoLocation)
                val transport = NextJsServerTransport(context)
                transport.send(context, vetoLocation.toAutoLocPayload(), "autoloc")
                context.log().i(TAG, "AutoLoc uploaded new location (Movement > 100m or initial run).")
            } else {
                context.log().i(TAG, "AutoLoc skipped upload (Movement <= 100m). Saved DB writes.")
            }
        } catch (e: Exception) {
            context.log().e(TAG, "Error in AutoLoc processLocation: ${e.message}")
        } finally {
            cleanup()
        }
    }

    private fun cleanup() {
        try {
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            // Ignore
        }
        deferred?.complete(Unit)
    }
}
