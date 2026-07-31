package com.neubofy.veto.data

import android.content.Context
import android.location.Location
import com.neubofy.veto.utils.Utils
import com.neubofy.veto.utils.Utils.Companion.getGoogleMapsLink
import java.util.Date


data class VetoLocation(
    val lat: Double,
    val lon: Double,

    /** Horizontal radius in meter */
    val accuracy: Float? = null,
    /** Height above sea level in meter */
    val altitude: Double? = null,
    /** Horizontal direction of travel between 0.0 and 360.0 */
    val bearing: Float? = null,
    /** Speed in m/s */
    val speed: Float? = null,

    val provider: String,
    val batteryLevel: Int,

    // Or this? -> Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
    val timeMillis: Long = System.currentTimeMillis(),
) {

    companion object {
        fun fromAndroidLocation(context: Context, loc: Location): VetoLocation {
            return VetoLocation(
                lat = loc.latitude,
                lon = loc.longitude,
                accuracy = if (loc.hasAccuracy()) loc.accuracy else null,
                altitude = if (loc.hasAltitude()) loc.altitude else null,
                bearing = if (loc.hasBearing()) loc.bearing else null,
                speed = if (loc.hasSpeed()) loc.speed else null,
                provider = loc.provider ?: "GPS",
                batteryLevel = Utils.getBatteryLevel(context),
                timeMillis = loc.time,
            )
        }
    }

    override fun toString(): String {
        val string = StringBuilder()
            .append("$provider:\n")
            .append("Lat: $lat\n")
            .append("Lon: $lon\n")

        if (accuracy != null) {
            string.append("Accuracy: %.1f m\n".format(accuracy))
        }
        if (altitude != null) {
            string.append("Altitude: %.1f m\n".format(altitude))
        }
        if (bearing != null) {
            string.append("Bearing: %.0f\n".format(bearing))
        }
        if (speed != null) {
            string.append("Speed: %.1f m/s = %.1f km/h\n".format(speed, speed * 3.6))
        }

        string.append("Time: ${Date(timeMillis)}\n")
            .append("Battery: $batteryLevel %\n")
            .append(getGoogleMapsLink(lat, lon))
        return string.toString()
    }
}
