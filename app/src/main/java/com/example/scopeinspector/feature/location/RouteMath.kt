package com.example.scopeinspector.feature.location

import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RoutePoint(val latitude: Double, val longitude: Double)

/** Una lectura de ubicación, sin depender de clases de Android para poder probar la lógica. */
data class LocationFix(
    val point: RoutePoint,
    val accuracyMeters: Float,
    val speedMetersPerSecond: Float?,
    val timeMillis: Long,
)

/** Cálculos y formatos de la ruta; todo puro para poder probarlo en CI. */
object RouteMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Por encima de este error (típico de una ubicación aproximada) la lectura no se usa para la ruta. */
    const val MAX_ROUTE_ACCURACY_METERS = 50f

    fun isUsableForRoute(accuracyMeters: Float): Boolean =
        accuracyMeters in 0f..MAX_ROUTE_ACCURACY_METERS

    /** Distancia sobre la superficie terrestre (fórmula de haversine). */
    fun distanceMeters(from: RoutePoint, to: RoutePoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_METERS * atan2(sqrt(a), sqrt(1 - a))
    }

    fun routeDistanceMeters(points: List<RoutePoint>): Double =
        points.zipWithNext { a, b -> distanceMeters(a, b) }.sum()

    fun formatDistance(meters: Double, locale: Locale = Locale.getDefault()): String =
        if (meters < 1000) {
            String.format(locale, "%.0f m", meters)
        } else {
            String.format(locale, "%.2f km", meters / 1000)
        }

    fun formatSpeedKmh(metersPerSecond: Float, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%.1f km/h", metersPerSecond * 3.6f)

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    fun formatCoordinates(point: RoutePoint): String =
        String.format(Locale.US, "%.5f, %.5f", point.latitude, point.longitude)
}
