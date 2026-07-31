package com.neubofy.veto.locationproviders

import kotlinx.coroutines.Deferred


abstract class LocationProvider {

    /**
     * Gets the location and sends it once it is available.
     *
     * This may take some time, e.g., to acquire a new GPS lock.
     * Therefore, if you override this function, you may want to create a new
     * thread to avoid blocking the caller.
     *
     * @return A Deferred that signals that the getting the location is complete.
     */
    abstract suspend fun getAndSendLocation(): Deferred<Unit>

    open fun onStopped() {
        // nothing to do, can be overridden
    }
}
