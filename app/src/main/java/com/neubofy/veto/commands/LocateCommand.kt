package com.neubofy.veto.commands

import android.content.Context
import android.location.LocationManager.GPS_PROVIDER
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.locationproviders.AddJobResult
import com.neubofy.veto.locationproviders.GpsLocationProvider
import com.neubofy.veto.locationproviders.LocationAutoOnOffHandler
import com.neubofy.veto.locationproviders.LocationProvider
import com.neubofy.veto.permissions.LocationPermission
import com.neubofy.veto.permissions.WriteSecureSettingsPermission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class LocateCommand(context: Context) : Command(context) {

    companion object {
        private val TAG = LocateCommand::class.simpleName
    }

    override val keyword = "locate"

    override val usage = "locate [last | gps]"

    @get:DrawableRes
    override val icon = R.drawable.ic_location

    @get:StringRes
    override val shortDescription = R.string.cmd_locate_description_short

    @get:StringRes
    override val longDescription = R.string.cmd_locate_description_long

    override val requiredPermissions = listOf(LocationPermission())

    override val optionalPermissions = listOf(WriteSecureSettingsPermission())

    // Fields for execution
    private var providers = mutableListOf<LocationProvider>()
    private val locOnOffHandler = LocationAutoOnOffHandler.getInstance(context)
    private var addJobResult: AddJobResult? = null
    private var deferred: CompletableDeferred<Unit>? = null

    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        // veto locate last
        if (args.contains("last")) {
            withContext(Dispatchers.IO) {
                val provider = GpsLocationProvider(context, transport, GPS_PROVIDER, null, keyword)
                provider.getLastKnownLocation()
            }
            return
        }

        addJobResult = locOnOffHandler.addJob()

        if (!addJobResult!!.isLocationOn) {
            context.log().w(
                TAG,
                "Cannot locate: Location is off"
            )
            transport.send(context, context.getString(R.string.cmd_locate_response_location_off), keyword)
            return
        }

        deferred = CompletableDeferred<Unit>()

        // Force GPS only
        providers.clear()
        providers.add(GpsLocationProvider(context, transport, GPS_PROVIDER, null, keyword))

        // run the providers and get the locations
        withContext(Dispatchers.IO) {
            providers
                .map { prov -> prov.getAndSendLocation() }
                .forEach { deferredProvider -> deferredProvider.await() }
        }
        deferred?.complete(Unit)
    }

    override fun onExecuteStopped() {
        super.onExecuteStopped()

        providers.forEach { it.onStopped() }
        addJobResult?.let {
            locOnOffHandler.removeJob(it.jobId)
        }
        deferred?.complete(Unit)
    }
}
