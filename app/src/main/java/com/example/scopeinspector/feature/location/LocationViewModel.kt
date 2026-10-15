package com.example.scopeinspector.feature.location

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Avisos que la pantalla traduce a un texto para el usuario. */
enum class LocationMessage {
    LocationDisabled,
    NoFix,
    Error,
    LowAccuracy,
}

data class LocationUiState(
    val current: LocationFix? = null,
    val route: List<RoutePoint> = emptyList(),
    val distanceMeters: Double = 0.0,
    val elapsedSeconds: Long = 0,
    val tracking: Boolean = false,
    val loading: Boolean = false,
    val message: LocationMessage? = null,
)

class LocationViewModel(private val source: LocationSource, private val clock: ElapsedClock = SystemElapsedClock) :
    ViewModel() {
    private val _state = MutableStateFlow(LocationUiState())
    val state: StateFlow<LocationUiState> = _state.asStateFlow()

    private var trackingJob: Job? = null
    private var timerJob: Job? = null

    /** Una sola lectura de la posición actual, sin iniciar el rastreo. */
    fun locateOnce(precise: Boolean) {
        if (_state.value.loading) return
        if (!source.isLocationEnabled()) {
            _state.update { it.copy(message = LocationMessage.LocationDisabled) }
            return
        }
        _state.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            val fix = try {
                source.currentFix(precise)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, message = LocationMessage.Error) }
                return@launch
            }
            _state.update {
                if (fix == null) {
                    it.copy(loading = false, message = LocationMessage.NoFix)
                } else {
                    it.copy(current = fix, loading = false, message = null)
                }
            }
        }
    }

    fun startTracking(precise: Boolean) {
        if (trackingJob?.isActive == true) return
        if (!source.isLocationEnabled()) {
            _state.update { it.copy(message = LocationMessage.LocationDisabled) }
            return
        }
        val startedAt = clock.elapsedMillis()
        _state.update {
            LocationUiState(current = it.current, tracking = true)
        }
        // El cronómetro se crea antes que la recogida: si el flujo falla de inmediato (con el
        // dispatcher inmediato de Main la recogida arranca de forma síncrona), ya existe y se cancela.
        stopTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val elapsed = (clock.elapsedMillis() - startedAt) / 1_000
                _state.update { it.copy(elapsedSeconds = elapsed) }
            }
        }
        trackingJob = source.fixes(precise)
            .onEach { fix -> onFix(fix) }
            .catch {
                stopTimer()
                _state.update { it.copy(tracking = false, message = LocationMessage.Error) }
            }
            .launchIn(viewModelScope)
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        stopTimer()
        _state.update { it.copy(tracking = false) }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun onFix(fix: LocationFix) {
        _state.update { state ->
            if (!RouteMath.isUsableForRoute(fix.accuracyMeters)) {
                state.copy(current = fix, message = LocationMessage.LowAccuracy)
            } else {
                val last = state.route.lastOrNull()
                val added = if (last == null) 0.0 else RouteMath.distanceMeters(last, fix.point)
                state.copy(
                    current = fix,
                    route = state.route + fix.point,
                    distanceMeters = state.distanceMeters + added,
                    message = null,
                )
            }
        }
    }

    companion object {
        /** Fábrica manual: crea el ViewModel con la implementación real de Play Services. */
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer { LocationViewModel(LocationRepository(context.applicationContext)) }
        }
    }
}
