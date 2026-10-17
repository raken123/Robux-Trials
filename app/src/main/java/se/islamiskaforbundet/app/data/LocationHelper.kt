package se.islamiskaforbundet.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

object LocationHelper {
    /** Caller must hold a location permission. Returns null if no fix within a few seconds. */
    @SuppressLint("MissingPermission")
    suspend fun current(context: Context): Location? {
        val lm = context.getSystemService(LocationManager::class.java)
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        val fresh = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && providers.isNotEmpty()) {
            withTimeoutOrNull(8000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    lm.getCurrentLocation(providers.first(), null, ContextCompat.getMainExecutor(context)) { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                }
            }
        } else null
        return fresh ?: providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    /** Closest Swedish city name for display, or coordinates when far from all of them. */
    fun describe(lat: Double, lng: Double): String {
        val nearest = SWEDISH_CITIES.minByOrNull { (it.lat - lat) * (it.lat - lat) + (it.lng - lng) * (it.lng - lng) }
        val result = FloatArray(1)
        if (nearest != null) {
            Location.distanceBetween(lat, lng, nearest.lat, nearest.lng, result)
            if (result[0] < 40_000) return nearest.name
        }
        return "%.2f°, %.2f°".format(lat, lng)
    }
}
