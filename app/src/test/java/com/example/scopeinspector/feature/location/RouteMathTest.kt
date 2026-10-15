package com.example.scopeinspector.feature.location

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteMathTest {

    private val madrid = RoutePoint(40.4168, -3.7038)
    private val barcelona = RoutePoint(41.3874, 2.1686)

    @Test
    fun distanceBetweenSamePointIsZero() {
        assertEquals(0.0, RouteMath.distanceMeters(madrid, madrid), 0.001)
    }

    @Test
    fun distanceMadridBarcelonaIsAbout505Km() {
        val km = RouteMath.distanceMeters(madrid, barcelona) / 1000
        assertEquals(505.0, km, 5.0)
    }

    @Test
    fun oneThousandthOfDegreeOfLatitudeIsAbout111Meters() {
        val a = RoutePoint(40.0, -3.0)
        val b = RoutePoint(40.001, -3.0)
        assertEquals(111.2, RouteMath.distanceMeters(a, b), 0.5)
    }

    @Test
    fun routeDistanceSumsSegments() {
        val a = RoutePoint(40.0, -3.0)
        val b = RoutePoint(40.001, -3.0)
        val c = RoutePoint(40.002, -3.0)
        val total = RouteMath.routeDistanceMeters(listOf(a, b, c))
        assertEquals(RouteMath.distanceMeters(a, b) + RouteMath.distanceMeters(b, c), total, 0.001)
    }

    @Test
    fun routeWithFewerThanTwoPointsHasNoDistance() {
        assertEquals(0.0, RouteMath.routeDistanceMeters(emptyList()), 0.0)
        assertEquals(0.0, RouteMath.routeDistanceMeters(listOf(madrid)), 0.0)
    }

    @Test
    fun preciseFixesAreUsableAndApproximateOnesAreNot() {
        assertTrue(RouteMath.isUsableForRoute(5f))
        assertTrue(RouteMath.isUsableForRoute(RouteMath.MAX_ROUTE_ACCURACY_METERS))
        assertFalse(RouteMath.isUsableForRoute(1500f))
        assertFalse(RouteMath.isUsableForRoute(-1f))
    }

    @Test
    fun formatsDistanceInMetersAndKilometers() {
        assertEquals("42 m", RouteMath.formatDistance(42.4, Locale.US))
        assertEquals("1.25 km", RouteMath.formatDistance(1250.0, Locale.US))
    }

    @Test
    fun formatsSpeedInKmh() {
        assertEquals("3.6 km/h", RouteMath.formatSpeedKmh(1f, Locale.US))
    }

    @Test
    fun formatsDurationWithAndWithoutHours() {
        assertEquals("00:07", RouteMath.formatDuration(7))
        assertEquals("02:05", RouteMath.formatDuration(125))
        assertEquals("1:01:01", RouteMath.formatDuration(3661))
    }

    @Test
    fun distanceJustBelowOneKilometerStaysInMeters() {
        assertEquals("999 m", RouteMath.formatDistance(999.4, Locale.US))
    }

    @Test
    fun distanceThatRoundsToOneThousandMetersSwitchesToKilometers() {
        // Antes salía "1000 m": se redondeaba a 1000 antes de decidir la unidad.
        assertEquals("1.00 km", RouteMath.formatDistance(999.5, Locale.US))
        assertEquals("1.00 km", RouteMath.formatDistance(999.99, Locale.US))
        assertEquals("1.00 km", RouteMath.formatDistance(1000.0, Locale.US))
    }

    @Test
    fun invalidDistancesAreShownAsZero() {
        assertEquals("0 m", RouteMath.formatDistance(-5.0, Locale.US))
        assertEquals("0 m", RouteMath.formatDistance(Double.NaN, Locale.US))
        assertEquals("0 m", RouteMath.formatDistance(Double.POSITIVE_INFINITY, Locale.US))
    }

    @Test
    fun distanceUsesTheLocaleDecimalSeparator() {
        assertEquals("1,25 km", RouteMath.formatDistance(1250.0, Locale.forLanguageTag("es-ES")))
    }

    @Test
    fun invalidSpeedsAreShownAsZero() {
        assertEquals("0.0 km/h", RouteMath.formatSpeedKmh(Float.NaN, Locale.US))
        assertEquals("0.0 km/h", RouteMath.formatSpeedKmh(-1f, Locale.US))
        assertEquals("0.0 km/h", RouteMath.formatSpeedKmh(0f, Locale.US))
    }

    @Test
    fun negativeDurationIsShownAsZero() {
        assertEquals("00:00", RouteMath.formatDuration(-5))
    }

    @Test
    fun durationOfMoreThanADayKeepsCountingHours() {
        assertEquals("100:00:00", RouteMath.formatDuration(360_000))
    }

    @Test
    fun distanceAcrossTheAntimeridianIsShortNotAroundTheWorld() {
        val west = RoutePoint(0.0, 179.9)
        val east = RoutePoint(0.0, -179.9)
        assertEquals(22.2, RouteMath.distanceMeters(west, east) / 1000, 0.1)
    }

    @Test
    fun distanceIsSymmetric() {
        assertEquals(
            RouteMath.distanceMeters(madrid, barcelona),
            RouteMath.distanceMeters(barcelona, madrid),
            0.001,
        )
    }

    @Test
    fun nanAccuracyIsNotUsableForRoute() {
        assertFalse(RouteMath.isUsableForRoute(Float.NaN))
    }

    @Test
    fun formatsCoordinatesWithFiveDecimals() {
        assertEquals("40.41680, -3.70380", RouteMath.formatCoordinates(madrid))
    }
}
