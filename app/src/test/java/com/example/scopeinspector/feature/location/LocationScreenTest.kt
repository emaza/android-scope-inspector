package com.example.scopeinspector.feature.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.scopeinspector.R
import com.example.scopeinspector.testing.grant
import com.example.scopeinspector.ui.theme.ScopeInspectorTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Pantalla de ubicación con un [FakeLocationSource] y el mapa sustituido por un registro de llamadas. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h3000dp")
class LocationScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val source = FakeLocationSource()
    private val clock = FakeElapsedClock()
    private lateinit var viewModel: LocationViewModel

    private class MapCall(val current: RoutePoint?, val route: List<RoutePoint>)

    private val mapCalls = mutableListOf<MapCall>()
    private val activity get() = composeRule.activity

    private fun string(id: Int, vararg args: Any): String = activity.getString(id, *args)

    private fun show() {
        viewModel = LocationViewModel(source, clock)
        composeRule.setContent {
            ScopeInspectorTheme {
                LocationScreen(
                    onBack = {},
                    viewModel = viewModel,
                    mapContent = { current, route, _: Modifier -> mapCalls += MapCall(current, route) },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun grantLocation(fine: Boolean = true, coarse: Boolean = true) {
        if (fine) activity.application.grant(Manifest.permission.ACCESS_FINE_LOCATION)
        if (coarse) activity.application.grant(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun tap(textRes: Int) {
        composeRule.onNodeWithText(string(textRes)).performClick()
        composeRule.waitForIdle()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun stopTracking() {
        composeRule.runOnUiThread { viewModel.stopTracking() }
    }

    // --- sin permiso -----------------------------------------------------------------------------

    @Test
    fun withoutPermissionItExplainsThatTheMapCannotLocateYou() {
        show()

        composeRule.onNodeWithText(string(R.string.msg_no_permission)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.access_none)).assertIsDisplayed()
    }

    @Test
    fun openingTheScreenDoesNotRequestLocationPermission() {
        show()

        assertNull(shadowOf(activity).lastRequestedPermission)
    }

    @Test
    fun tappingLocateNowWithoutPermissionRequestsFineAndCoarseLocation() {
        show()

        tap(R.string.action_locate_now)

        val requested = shadowOf(activity).lastRequestedPermission
        assertNotNull(requested)
        assertEquals(
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            requested!!.requestedPermissions.toList(),
        )
        assertEquals("aún no se ha leído la ubicación", 0, source.currentFixCalls)
    }

    @Test
    fun tappingStartTrackingWithoutPermissionRequestsItToo() {
        show()

        tap(R.string.action_start_tracking)

        assertNotNull(shadowOf(activity).lastRequestedPermission)
        assertEquals(0, source.fixesSubscriptions)
    }

    @Test
    fun theActionRunsByItselfAfterTheUserGrantsThePermission() {
        source.currentFixResult = fix()
        show()
        tap(R.string.action_locate_now)
        val requested = shadowOf(activity).lastRequestedPermission!!

        grantLocation()
        composeRule.runOnUiThread {
            activity.onRequestPermissionsResult(
                requested.requestCode,
                requested.requestedPermissions,
                IntArray(requested.requestedPermissions.size) { PackageManager.PERMISSION_GRANTED },
            )
        }
        waitForText("40.00000, -3.00000")

        assertEquals(1, source.currentFixCalls)
    }

    @Test
    fun theActionDoesNotRunIfTheUserDeniesThePermission() {
        source.currentFixResult = fix()
        show()
        tap(R.string.action_locate_now)
        val requested = shadowOf(activity).lastRequestedPermission!!

        composeRule.runOnUiThread {
            activity.onRequestPermissionsResult(
                requested.requestCode,
                requested.requestedPermissions,
                IntArray(requested.requestedPermissions.size) { PackageManager.PERMISSION_DENIED },
            )
        }
        composeRule.waitForIdle()

        assertEquals(0, source.currentFixCalls)
        composeRule.onNodeWithText(string(R.string.msg_no_permission)).assertIsDisplayed()
    }

    // --- con permiso ---------------------------------------------------------------------------------

    @Test
    fun withPermissionAndNoFixItInvitesToLocateYou() {
        grantLocation()
        show()

        composeRule.onNodeWithText(string(R.string.location_no_fix_hint)).assertIsDisplayed()
    }

    @Test
    fun locateNowShowsThePositionAccuracyAndPreciseAccess() {
        grantLocation()
        source.currentFixResult = fix(accuracy = 5f)
        show()

        tap(R.string.action_locate_now)
        waitForText("40.00000, -3.00000")

        composeRule.onNodeWithText(string(R.string.accuracy_value, 5)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.access_precise)).assertIsDisplayed()
        assertEquals(listOf(true), source.requestedPrecise)
    }

    @Test
    fun withOnlyApproximatePermissionItIsReportedAndAsksForBalancedAccuracy() {
        grantLocation(fine = false, coarse = true)
        source.currentFixResult = fix(accuracy = 1_200f)
        show()

        tap(R.string.action_locate_now)
        waitForText("40.00000, -3.00000")

        composeRule.onNodeWithText(string(R.string.access_approximate)).assertIsDisplayed()
        assertEquals(listOf(false), source.requestedPrecise)
    }

    @Test
    fun withTheDeviceLocationOffItShowsTheMessage() {
        grantLocation()
        source.enabled = false
        show()

        tap(R.string.action_locate_now)

        composeRule.onNodeWithText(string(R.string.msg_location_disabled)).assertIsDisplayed()
        assertEquals(0, source.currentFixCalls)
    }

    @Test
    fun whenNoFixCanBeObtainedItSaysSo() {
        grantLocation()
        source.currentFixResult = null
        show()

        tap(R.string.action_locate_now)

        composeRule.onNodeWithText(string(R.string.msg_no_fix)).assertIsDisplayed()
    }

    // --- rastreo ---------------------------------------------------------------------------------------

    @Test
    fun trackingSwapsTheStartButtonForTheStopButtonAndBack() {
        grantLocation()
        show()

        tap(R.string.action_start_tracking)
        composeRule.onNodeWithText(string(R.string.action_stop_tracking)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_start_tracking)).assertDoesNotExist()

        tap(R.string.action_stop_tracking)
        composeRule.onNodeWithText(string(R.string.action_start_tracking)).assertIsDisplayed()
    }

    @Test
    fun trackingShowsTheDistanceCovered() {
        grantLocation()
        show()
        tap(R.string.action_start_tracking)

        composeRule.runOnUiThread {
            source.fixesFlow.tryEmit(fix(latitudeOffset = 0.000))
            source.fixesFlow.tryEmit(fix(latitudeOffset = 0.001))
        }
        // 0,001 grados de latitud son unos 111 m
        waitForText("111 m")

        assertEquals(2, mapCalls.last().route.size)
    }

    @Test
    fun theMapReceivesTheCurrentPositionAndTheRoute() {
        grantLocation()
        show()
        tap(R.string.action_start_tracking)

        composeRule.runOnUiThread {
            source.fixesFlow.tryEmit(fix(latitudeOffset = 0.000))
            source.fixesFlow.tryEmit(fix(latitudeOffset = 0.002))
        }
        waitForText("222 m")

        val last = mapCalls.last()
        assertEquals(RoutePoint(40.002, -3.0), last.current)
        assertEquals(2, last.route.size)
    }

    @Test
    fun anImpreciseFixShowsTheWarningWithoutGrowingTheRoute() {
        grantLocation(fine = false, coarse = true)
        show()
        tap(R.string.action_start_tracking)

        composeRule.runOnUiThread { source.fixesFlow.tryEmit(fix(accuracy = 1_500f)) }
        waitForText(string(R.string.msg_low_accuracy))

        assertTrue(mapCalls.last().route.isEmpty())
    }
}
