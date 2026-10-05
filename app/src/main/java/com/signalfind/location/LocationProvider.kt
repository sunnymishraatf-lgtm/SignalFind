package com.signalfind.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Best-effort location for the recovery SMS path. Works fully offline:
 * GNSS needs no internet; the fused provider may additionally use network
 * location when connectivity exists, and degrades gracefully without it.
 *
 * Honesty contract: [LocationResult.Live] is only returned for a fix that is
 * fresh (<= 60 s old). Anything older is [LocationResult.Cached] with its age.
 * Nothing is ever invented — [LocationResult.Unavailable] explains why.
 */
class LocationProvider(private val context: Context) {

    sealed interface LocationResult {
        data class Live(
            val latitude: Double,
            val longitude: Double,
            val accuracyM: Float,
            val timeMs: Long,
            val provider: String
        ) : LocationResult

        data class Cached(
            val latitude: Double,
            val longitude: Double,
            val accuracyM: Float?,
            val timeMs: Long,
            val provider: String
        ) : LocationResult

        data class Unavailable(val reason: String) : LocationResult
    }

    companion object {
        /** A last-known fix newer than this counts as live. */
        const val FRESH_MS = 60_000L
        /** How long we wait for a fresh GNSS fix before falling back. */
        const val FRESH_TIMEOUT_MS = 20_000L
    }

    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return false
        // An SMS arrives while the app is in the background, so background
        // location is required on Android 10+.
        if (Build.VERSION.SDK_INT >= 29) {
            val bg = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!bg) return false
        }
        return true
    }

    private fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(LocationManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= 28) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    suspend fun getBestLocation(timeoutMs: Long = FRESH_TIMEOUT_MS): LocationResult =
        // Tasks.await() blocks the thread; keep it off the Default pool.
        withContext(Dispatchers.IO) {
            if (!hasPermission()) {
                return@withContext LocationResult.Unavailable("Location permission not granted")
            }
            if (!isLocationEnabled()) {
                return@withContext LocationResult.Unavailable("Location services are disabled")
            }

            val cached = lastKnown()
            if (cached != null && System.currentTimeMillis() - cached.time <= FRESH_MS) {
                return@withContext LocationResult.Live(
                    cached.latitude, cached.longitude,
                    cached.accuracy, cached.time,
                    providerLabel(cached.provider)
                )
            }

            val fresh = requestFreshFix(timeoutMs)
            if (fresh != null) {
                // getCurrentLocation may hand back a recent cached fix; only
                // call it live when it is actually fresh.
                val age = System.currentTimeMillis() - fresh.time
                if (age <= FRESH_MS) {
                    return@withContext LocationResult.Live(
                        fresh.latitude, fresh.longitude,
                        fresh.accuracy, fresh.time,
                        providerLabel(fresh.provider)
                    )
                }
                return@withContext LocationResult.Cached(
                    fresh.latitude, fresh.longitude,
                    fresh.accuracy.takeIf { it > 0f }, fresh.time,
                    providerLabel(fresh.provider)
                )
            }

            if (cached != null) {
                return@withContext LocationResult.Cached(
                    cached.latitude, cached.longitude,
                    cached.accuracy.takeIf { it > 0f }, cached.time,
                    providerLabel(cached.provider)
                )
            }

            LocationResult.Unavailable("Could not obtain a location fix (weak signal?)")
        }

    private suspend fun lastKnown(): Location? {
        // Fused last location first; fall back to LocationManager providers so
        // devices without Play Services still get a cached fix.
        try {
            fused.lastLocation.await()?.let { return it }
        } catch (_: Exception) {
        }
        return try {
            val lm = context.getSystemService(LocationManager::class.java) ?: return null
            val providers = lm.getProviders(true)
            var best: Location? = null
            for (p in providers) {
                @Suppress("DEPRECATION")
                val l = try {
                    lm.getLastKnownLocation(p)
                } catch (_: SecurityException) {
                    null
                }
                if (l != null && (best == null || l.time > best.time)) best = l
            }
            best
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun requestFreshFix(timeoutMs: Long): Location? {
        return try {
            withTimeoutOrNull(timeoutMs) {
                val token = CancellationTokenSource()
                try {
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
                } finally {
                    token.cancel()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun providerLabel(provider: String?): String = when (provider) {
        LocationManager.GPS_PROVIDER -> "GPS"
        LocationManager.NETWORK_PROVIDER -> "Network"
        LocationManager.FUSED_PROVIDER -> "Fused"
        else -> "Location"
    }
}
