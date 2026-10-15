package com.example.scopeinspector.feature.location

import com.example.scopeinspector.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

@OptIn(ExperimentalCoroutinesApi::class)
class LocationViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    // Red de seguridad: si algo se cuelga, el test falla en vez de girar indefinidamente.
    @get:Rule
    val timeout: Timeout = Timeout.seconds(30)

    private val source = FakeLocationSource()
    private val clock = FakeElapsedClock()
    private val viewModel = LocationViewModel(source, clock)
    private val state get() = viewModel.state.value

    /**
     * `runTest` espera a que se vacíe el planificador al terminar, y el cronómetro del rastreo es un
     * bucle infinito con `delay`. Se detiene el rastreo al acabar para que el test termine.
     */
    private fun runVmTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            viewModel.stopTracking()
        }
    }

    // --- estado inicial -----------------------------------------------------------------------

    @Test
    fun startsEmptyAndIdle() {
        assertEquals(LocationUiState(), state)
    }

    // --- locateOnce ---------------------------------------------------------------------------

    @Test
    fun locateOnceWithLocationDisabledShowsMessageAndDoesNotAskTheSource() = runVmTest {
        source.enabled = false

        viewModel.locateOnce(precise = true)
        runCurrent()

        assertEquals(LocationMessage.LocationDisabled, state.message)
        assertFalse(state.loading)
        assertEquals(0, source.currentFixCalls)
    }

    @Test
    fun locateOnceShowsLoadingAndThenStoresTheFix() = runVmTest {
        val expected = fix(latitudeOffset = 0.5)
        source.currentFixResult = expected

        viewModel.locateOnce(precise = true)
        assertTrue("debe estar cargando antes de que responda la fuente", state.loading)
        runCurrent()

        assertEquals(expected, state.current)
        assertFalse(state.loading)
        assertNull(state.message)
    }

    @Test
    fun locateOnceWithoutFixShowsNoFixMessage() = runVmTest {
        source.currentFixResult = null

        viewModel.locateOnce(precise = true)
        runCurrent()

        assertEquals(LocationMessage.NoFix, state.message)
        assertNull(state.current)
        assertFalse(state.loading)
    }

    @Test
    fun locateOnceWhenTheSourceFailsShowsErrorAndStopsLoading() = runVmTest {
        source.currentFixError = IllegalStateException("sin servicios de Google")

        viewModel.locateOnce(precise = true)
        runCurrent()

        assertEquals(LocationMessage.Error, state.message)
        assertFalse(state.loading)
    }

    @Test
    fun locateOnceIgnoresACallWhileAnotherIsStillLoading() = runVmTest {
        val gate = CompletableDeferred<Unit>()
        source.currentFixGate = gate
        source.currentFixResult = fix()

        viewModel.locateOnce(precise = true)
        runCurrent()
        viewModel.locateOnce(precise = true)
        runCurrent()
        assertEquals(1, source.currentFixCalls)

        gate.complete(Unit)
        runCurrent()
        assertFalse(state.loading)
    }

    @Test
    fun locateOnceClearsAPreviousMessage() = runVmTest {
        source.currentFixResult = null
        viewModel.locateOnce(precise = true)
        runCurrent()
        assertEquals(LocationMessage.NoFix, state.message)

        source.currentFixResult = fix()
        viewModel.locateOnce(precise = true)
        runCurrent()

        assertNull(state.message)
    }

    @Test
    fun locateOnceDoesNotStartTrackingNorTouchTheRoute() = runVmTest {
        source.currentFixResult = fix()

        viewModel.locateOnce(precise = true)
        runCurrent()

        assertFalse(state.tracking)
        assertTrue(state.route.isEmpty())
        assertEquals(0, source.fixesSubscriptions)
    }

    @Test
    fun locateOncePassesThePrecisionToTheSource() = runVmTest {
        source.currentFixResult = fix()

        viewModel.locateOnce(precise = false)
        runCurrent()

        assertEquals(listOf(false), source.requestedPrecise)
    }

    // --- rastreo: arranque --------------------------------------------------------------------

    @Test
    fun startTrackingWithLocationDisabledShowsMessageAndDoesNotTrack() = runVmTest {
        source.enabled = false

        viewModel.startTracking(precise = true)
        runCurrent()

        assertEquals(LocationMessage.LocationDisabled, state.message)
        assertFalse(state.tracking)
        assertEquals(0, source.fixesSubscriptions)
    }

    @Test
    fun startTrackingMarksTrackingAndKeepsTheLastKnownFix() = runVmTest {
        val known = fix(latitudeOffset = 0.1)
        source.currentFixResult = known
        viewModel.locateOnce(precise = true)
        runCurrent()

        viewModel.startTracking(precise = true)
        runCurrent()

        assertTrue(state.tracking)
        assertEquals(known, state.current)
        assertTrue(state.route.isEmpty())
    }

    @Test
    fun startTrackingTwiceDoesNotSubscribeTwice() = runVmTest {
        viewModel.startTracking(precise = true)
        runCurrent()
        viewModel.startTracking(precise = true)
        runCurrent()

        assertEquals(1, source.fixesSubscriptions)
        assertEquals(1, source.requestedPrecise.size)
    }

    @Test
    fun startTrackingPassesThePrecisionToTheSource() = runVmTest {
        viewModel.startTracking(precise = false)
        runCurrent()

        assertEquals(listOf(false), source.requestedPrecise)
    }

    // --- rastreo: lecturas --------------------------------------------------------------------

    @Test
    fun fixesBuildTheRouteAndAccumulateTheDistance() = runVmTest {
        viewModel.startTracking(precise = true)
        runCurrent()

        source.fixesFlow.emit(fix(latitudeOffset = 0.000))
        source.fixesFlow.emit(fix(latitudeOffset = 0.001))
        source.fixesFlow.emit(fix(latitudeOffset = 0.002))
        runCurrent()

        assertEquals(3, state.route.size)
        // 0,001 grados de latitud son unos 111,2 m
        assertEquals(222.4, state.distanceMeters, 1.0)
        assertEquals(fix(latitudeOffset = 0.002), state.current)
    }

    @Test
    fun theFirstFixAddsNoDistance() = runVmTest {
        viewModel.startTracking(precise = true)
        runCurrent()

        source.fixesFlow.emit(fix())
        runCurrent()

        assertEquals(1, state.route.size)
        assertEquals(0.0, state.distanceMeters, 0.0)
    }

    @Test
    fun anImpreciseFixUpdatesTheCurrentPositionButNotTheRouteAndWarns() = runVmTest {
        viewModel.startTracking(precise = true)
        runCurrent()
        source.fixesFlow.emit(fix(latitudeOffset = 0.0))
        runCurrent()

        val imprecise = fix(latitudeOffset = 0.01, accuracy = 1_500f)
        source.fixesFlow.emit(imprecise)
        runCurrent()

        assertEquals(1, state.route.size)
        assertEquals(0.0, state.distanceMeters, 0.0)
        assertEquals(imprecise, state.current)
        assertEquals(LocationMessage.LowAccuracy, state.message)
    }

    @Test
    fun aGoodFixClearsTheLowAccuracyWarning() = runVmTest {
        viewModel.startTracking(precise = true)
        runCurrent()
        source.fixesFlow.emit(fix(accuracy = 1_500f))
        runCurrent()
        assertEquals(LocationMessage.LowAccuracy, state.message)

        source.fixesFlow.emit(fix(accuracy = 5f))
        runCurrent()

        assertNull(state.message)
    }

    @Test
    fun theAccuracyLimitIsInclusive() = runVmTest {
        viewModel.startTracking(precise = true)
        runCurrent()

        source.fixesFlow.emit(fix(accuracy = RouteMath.MAX_ROUTE_ACCURACY_METERS))
        source.fixesFlow.emit(fix(latitudeOffset = 0.001, accuracy = RouteMath.MAX_ROUTE_ACCURACY_METERS + 0.1f))
        runCurrent()

        assertEquals(1, state.route.size)
    }

    // --- rastreo: parada y reinicio ---------------------------------------------------------------

    @Test
    fun stopTrackingKeepsTheRouteAndStopsCollecting() = runVmTest {
        viewModel.startTracking(precise = true)
        runCurrent()
        source.fixesFlow.emit(fix(latitudeOffset = 0.000))
        source.fixesFlow.emit(fix(latitudeOffset = 0.001))
        runCurrent()

        viewModel.stopTracking()
        source.fixesFlow.emit(fix(latitudeOffset = 0.002))
        runCurrent()

        assertFalse(state.tracking)
        assertEquals(2, state.route.size)
    }

    @Test
    fun startingAgainResetsTheRouteTheDistanceAndTheTimerButKeepsTheLastFix() = runVmTest {
        viewModel.startTracking(precise = true)
        runCurrent()
        source.fixesFlow.emit(fix(latitudeOffset = 0.000))
        source.fixesFlow.emit(fix(latitudeOffset = 0.001))
        runCurrent()
        clock.nowMillis = 4_000
        advanceTimeBy(4_000)
        runCurrent()
        viewModel.stopTracking()
        val last = state.current

        viewModel.startTracking(precise = true)
        runCurrent()

        assertTrue(state.route.isEmpty())
        assertEquals(0.0, state.distanceMeters, 0.0)
        assertEquals(0L, state.elapsedSeconds)
        assertEquals(last, state.current)
        assertTrue(state.tracking)
    }

    // --- cronómetro ---------------------------------------------------------------------------

    @Test
    fun theTimerCountsWholeSecondsFromTheClock() = runVmTest {
        viewModel.startTracking(precise = true)
        runCurrent()

        clock.nowMillis = 3_000
        advanceTimeBy(3_000)
        runCurrent()
        assertEquals(3L, state.elapsedSeconds)

        clock.nowMillis = 5_500
        advanceTimeBy(2_500)
        runCurrent()
        assertEquals(5L, state.elapsedSeconds)
    }

    @Test
    fun theTimerStartsFromTheClockValueAtTheMomentTrackingStarts() = runVmTest {
        clock.nowMillis = 90_000
        viewModel.startTracking(precise = true)
        runCurrent()

        clock.nowMillis = 92_000
        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(2L, state.elapsedSeconds)
    }

    @Test
    fun theTimerStopsWhenTrackingIsStopped() = runVmTest {
        viewModel.startTracking(precise = true)
        runCurrent()
        clock.nowMillis = 2_000
        advanceTimeBy(2_000)
        runCurrent()
        viewModel.stopTracking()

        clock.nowMillis = 10_000
        advanceTimeBy(8_000)
        runCurrent()

        assertEquals(2L, state.elapsedSeconds)
    }

    // --- fallos del flujo de ubicación --------------------------------------------------------------

    @Test
    fun aFailureInTheLocationStreamStopsTrackingAndShowsAnError() = runVmTest {
        source.fixesError = SecurityException("permiso revocado")

        viewModel.startTracking(precise = true)
        runCurrent()

        assertFalse(state.tracking)
        assertEquals(LocationMessage.Error, state.message)
    }

    @Test
    fun theTimerStopsWhenTheLocationStreamFails() = runVmTest {
        source.fixesError = SecurityException("permiso revocado")
        viewModel.startTracking(precise = true)
        runCurrent()

        clock.nowMillis = 10_000
        advanceTimeBy(10_000)
        runCurrent()

        assertEquals("el cronómetro no debe seguir contando tras un error", 0L, state.elapsedSeconds)
    }

    @Test
    fun trackingCanStartAgainAfterAFailure() = runVmTest {
        source.fixesError = SecurityException("permiso revocado")
        viewModel.startTracking(precise = true)
        runCurrent()

        source.fixesError = null
        viewModel.startTracking(precise = true)
        runCurrent()

        assertTrue(state.tracking)
        assertEquals(2, source.fixesSubscriptions)
    }
}
