package com.neubofy.veto.locationproviders

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.neubofy.veto.R
import com.neubofy.veto.data.VetoLocation
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.permissions.LocationPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt


// Testing on GrapheneOS has shown that if you are in a reasonable place for GPS
// (open sky, in a car/bus/train, in a building near a window, ...),
// you usually get a GPS position in < 1 minute. Often in < 30 seconds.
//
// Therefore, set the timeout to 2 minutes.
// It's better to fail fast, inform the user, and let them decide what to do
// (Resend "locate gps"? Send "locate cell" instead?).
// A shorter timeout also reduces the battery impact for commands that won't succeed anyway.
// And it reduces the risk of Android punishing Veto for excessive foreground service usage.
const val MAX_GPS_DURATION_MILLIS = 2 * 60 * 1000L

private const val UPDATE_INTERVAL_MILLIS = 2 * 1000L

// LocationManager.FUSED_PROVIDER was only added in SDK 31
const val FUSED_PROVIDER = "fused"

/**
 * Only call this provider via the LocateCommand!
 * (because it handles things like LocationAutoOnOff centrally)
 */
class GpsLocationProvider<T>(
    private val context: Context,
    private val transport: Transport<T>,
    private var requestedProvider: String,
    private val requestedAccuracy: Int?,
    private val commandName: String? = null
) : LocationProvider(), LocationListener {

    companion object {
        private val TAG = GpsLocationProvider::class.simpleName
    }

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var deferred: CompletableDeferred<Unit>? = null

    private var currBestLocation: VetoLocation? = null
    private var locationCount = 0
    private var previousAccuracy = 0F

    private var coroutineScope: CoroutineScope? = null

    @SuppressLint("MissingPermission") // linter is not good enough to recognize the check
    override suspend fun getAndSendLocation(): Deferred<Unit> {
        val def = CompletableDeferred<Unit>()
        deferred = def

        if (!LocationPermission().isGranted(context)) {
            context.log().i(TAG, "Missing location permission, cannot get location")
            def.complete(Unit)
            return def
        }

        // Countdown to stop the job after some timeout
        // Job() is important! Without it, if we coroutineScope.cancel() during cleanup,
        // it will cancel the parent CoroutineScope as well.
        // We don't want that, since other location providers may be pending in that scope, too.
        coroutineScope = CoroutineScope(currentCoroutineContext() + Job())

        coroutineScope?.launch(Dispatchers.IO) {
            delay(MAX_GPS_DURATION_MILLIS - 5_000)

            context.log().d(TAG, "Stopping locating due to timeout. Sending best location so far.")
            sendBestLocationAndFinish()
        }

        // Accuracy is only really meaningful with GPS, which can improve over time.
        if (requestedAccuracy != null) {
            context.log().w(TAG, "Forcing GPS provider since accuracy is set.")
            requestedProvider = LocationManager.GPS_PROVIDER
        }

        if (!locationManager.isProviderEnabled(requestedProvider)) {
            val msg = context.getString(R.string.cmd_locate_provider_disabled, requestedProvider)
            context.log().d(TAG, msg)
            transport.send(context, msg, commandName)
            def.complete(Unit)
            return def
        }

        context.log()
            .d(TAG, "Requesting location from $requestedProvider with accuracy $requestedAccuracy")
        locationManager.requestLocationUpdates(
            requestedProvider,
            UPDATE_INTERVAL_MILLIS,
            0f,
            this,
            Looper.getMainLooper(),
        )

        transport.send(context, context.getString(R.string.cmd_locate_response_gps_will_follow), commandName)
        return def
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(asFallBackForCurrentLocation: Boolean = false) {
        val lastLocationFromAndroid =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        val settings = SettingsRepository.getInstance(context)
        val cachedLoc = settings.getLastKnownLocation()

        if (lastLocationFromAndroid == null) {
            if (asFallBackForCurrentLocation) {
                // If current location was requested originally, don't fall back to cached location.
                transport.send(context, context.getString(R.string.cmd_locate_response_gps_fail), commandName)
            } else if (cachedLoc != null) {
                // If last location was requested, fall back to cached location.
                transport.sendNewLocation(context, cachedLoc, commandName)
            } else {
                // No location and nothing to fall back to.
                transport.send(
                    context,
                    context.getString(R.string.cmd_locate_last_known_location_not_available),
                    commandName
                )
            }
        } else {
            if (cachedLoc == null) {
                // Update our cached location
                onLocationChanged(lastLocationFromAndroid)
            } else {
                // If the last location from the LocationManager is newer than our cached location,
                // update our cached location.
                if (lastLocationFromAndroid.time > cachedLoc.timeMillis) {
                    onLocationChanged(lastLocationFromAndroid)
                } else {
                    transport.sendNewLocation(context, cachedLoc, commandName)
                }
            }
        }
        cleanup()
    }

    override fun onLocationChanged(location: Location) {
        val vetoLocation = VetoLocation.fromAndroidLocation(context, location)
        context.log().d(
            TAG,
            "Location found by ${vetoLocation.provider} with accuracy ${vetoLocation.accuracy} m."
        )

        locationCount += 1
        val isAccDiffLarge = isAccuracyDiffLarge(vetoLocation)
        updateCurrentBestLocation(vetoLocation)

        if (requestedAccuracy != null) {
            // If good enough: send location and finish
            val currBest = currBestLocation
            val currAccuracy = currBest?.accuracy?.roundToInt()
            if (currAccuracy != null && currAccuracy <= requestedAccuracy) {
                transport.sendNewLocation(context, currBest, commandName)
                cleanup()
                return
            }
        } else {
            // Skip a few initial locations to wait for a more accurate GPS-based location.
            // Wait either until the accuracy does not improve anymore or for a fixed number of results.
            if (requestedProvider == LocationManager.GPS_PROVIDER
                && isAccDiffLarge
                && locationCount < 15
            ) {
                return
            }
            // Return this location and finish
            val settings = SettingsRepository.getInstance(context)
            settings.storeLastKnownLocation(vetoLocation)
            transport.sendNewLocation(context, vetoLocation, commandName)
            cleanup()
        }
    }

    private fun isAccuracyDiffLarge(vetoLocation: VetoLocation): Boolean {
        if (vetoLocation.accuracy == null) {
            return false
        }
        val diff = (previousAccuracy - vetoLocation.accuracy).absoluteValue
        if (diff == 0F) {
            // This is very likely the same location => Keep running.
            return false
        }
        previousAccuracy = vetoLocation.accuracy
        return diff > 5 // meter
    }

    private fun updateCurrentBestLocation(newLocation: VetoLocation) {
        val currBest = currBestLocation
        if (
        // any new location is better than no accuracy
            currBest == null || currBest.accuracy == null
            // if we already have a current best: is the new location better?
            || (newLocation.accuracy != null && newLocation.accuracy < currBest.accuracy)
        ) {
            currBestLocation = newLocation
            val settings = SettingsRepository.getInstance(context)
            settings.storeLastKnownLocation(newLocation)
        }
    }

    private fun sendBestLocationAndFinish() {
        val currBest = currBestLocation
        if (currBest != null) {
            transport.sendNewLocation(context, currBest, commandName)
        } else {
            val msg = context.getString(R.string.cmd_locate_response_gps_fail)
            context.log().d(TAG, msg)
            transport.send(context, msg, commandName)
        }
        cleanup()
    }

    private fun cleanup() {
        locationManager.removeUpdates(this)
        coroutineScope?.cancel()
        deferred?.complete(Unit)
    }

    override fun onStopped() {
        cleanup()
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        // unused
    }

    override fun onProviderEnabled(provider: String) {
        // unused
    }

    override fun onProviderDisabled(provider: String) {
        // unused
    }
}
