package com.example.util

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Process
import androidx.core.content.ContextCompat
import java.security.SecureRandom
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real Android Platform APIs for Anchor Family:
 * - Cryptographically secure pairing code generation (java.security.SecureRandom)
 * - Screen Time inspection via UsageStatsManager & AppOpsManager
 * - Installed App discovery via PackageManager
 * - Real Device GPS location capture via LocationManager + Haversine geofence math
 * - Time-zone-aware schedule evaluation
 * - Network connectivity state verification via ConnectivityManager
 */
object AndroidPlatformHelper {

    private val secureRandom = SecureRandom()

    fun generateSecurePairingCode(): String {
        val part1 = 100 + secureRandom.nextInt(900)
        val part2 = 100 + secureRandom.nextInt(900)
        return String.format(Locale.US, "%03d-%03d", part1, part2)
    }

    fun normalizePairingCode(rawCode: String): String? {
        val digits = rawCode.filter { it.isDigit() }
        if (digits.length != 6) return null
        return "${digits.substring(0, 3)}-${digits.substring(3, 6)}"
    }

    fun isNetworkOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun queryDeviceScreenTimeMinutesToday(context: Context): Int? {
        if (!hasUsageStatsPermission(context)) return null
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                cal.timeInMillis,
                System.currentTimeMillis()
            )
            val totalMs = stats?.sumOf { it.totalTimeInForeground } ?: 0L
            (totalMs / 60_000L).toInt().coerceIn(0, 1440)
        } catch (_: Exception) {
            null
        }
    }

    fun queryInstalledLaunchableAppsCount(context: Context): Int {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            pm.queryIntentActivities(mainIntent, 0).size
        } catch (_: Exception) {
            0
        }
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @Suppress("MissingPermission")
    fun captureDeviceGpsLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            providers.firstNotNullOfOrNull { provider ->
                if (lm.isProviderEnabled(provider)) {
                    lm.getLastKnownLocation(provider)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Haversine formula to compute great-circle distance in meters between two WGS84 coordinates.
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Int {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadiusMeters * c).roundToInt()
    }

    fun isInsideGeofence(
        childLat: Double,
        childLon: Double,
        geofenceLat: Double,
        geofenceLon: Double,
        radiusMeters: Int
    ): Boolean {
        val distance = calculateDistanceMeters(childLat, childLon, geofenceLat, geofenceLon)
        return distance <= radiusMeters
    }
}
