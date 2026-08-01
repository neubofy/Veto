package com.neubofy.veto.utils

import android.content.Context
import android.location.Location
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.neubofy.veto.data.Settings
import com.neubofy.veto.data.SettingsRepository
import com.neubofy.veto.data.VetoLocation

object LocationHistoryManager {
    private const val TAG = "LocationHistoryManager"
    private const val MAX_HISTORY_SIZE = 5
    private const val MIN_DISTANCE_THRESHOLD_METERS = 100f

    /**
     * Filters new location updates using a 100-meter radius threshold.
     * Caches the last 5 distinct locations locally and in cloud updates.
     * @return true if location moved >= 100m (or first location) and should be updated; false if negligible change (< 100m).
     */
    fun processNewLocation(context: Context, newLoc: VetoLocation): Boolean {
        val settings = SettingsRepository.getInstance(context)
        val historyJson = settings.get(Settings.SET_LAST_KNOWN_LOCATION_HISTORY) as? String ?: "[]"

        val historyList: MutableList<VetoLocation> = try {
            val type = object : TypeToken<MutableList<VetoLocation>>() {}.type
            Gson().fromJson(historyJson, type) ?: mutableListOf()
        } catch (_: Exception) {
            mutableListOf()
        }

        // Check 100 meter radius threshold against last recorded location
        val lastLoc = historyList.lastOrNull()
        if (lastLoc != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                lastLoc.lat, lastLoc.lon,
                newLoc.lat, newLoc.lon,
                results
            )
            val distanceMeters = results[0]
            if (distanceMeters < MIN_DISTANCE_THRESHOLD_METERS) {
                context.log().i(TAG, "Negligible movement ($distanceMeters m < 100m). Skipping duplicate location update.")
                return false
            }
        }

        // Record location and cap history at last 5 entries
        historyList.add(newLoc)
        while (historyList.size > MAX_HISTORY_SIZE) {
            historyList.removeAt(0)
        }

        val updatedJson = Gson().toJson(historyList)
        settings.set(Settings.SET_LAST_KNOWN_LOCATION_HISTORY, updatedJson)
        settings.storeLastKnownLocation(newLoc)
        context.log().i(TAG, "Recorded new location (${newLoc.lat}, ${newLoc.lon}). Total cached entries: ${historyList.size}")
        return true
    }

    fun getLast5Locations(context: Context): List<VetoLocation> {
        val settings = SettingsRepository.getInstance(context)
        val historyJson = settings.get(Settings.SET_LAST_KNOWN_LOCATION_HISTORY) as? String ?: "[]"
        return try {
            val type = object : TypeToken<List<VetoLocation>>() {}.type
            Gson().fromJson(historyJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
