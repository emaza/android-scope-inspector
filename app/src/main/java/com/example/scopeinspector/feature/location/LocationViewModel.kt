package com.example.scopeinspector.feature.location

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

class LocationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LocationRepository(application)
    private val _state = MutableStateFlow(LocationUiState())
    val state: StateFlow<LocationUiState> = _state.asStateFlow()

    private var trackingJob: Job? = null
    private var timerJob: Job? = null

    /** Una sola lectura de la posición actual, sin iniciar el rastreo. */
    fun locateOnce(precise: Boolean) {
        if (_state.value.loading) return
        if (!repository.isLocationEnabled()) {
            _state.update { it.copy(message = LocationMessage.LocationDisabled) }
            return
        }
        _state.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            val fix = try {
                repository.currentFix(precise)
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
        if (!repository.isLocationEnabled()) {
            _state.update { it.copy(message = LocationMessage.LocationDisabled) }
            return
        }
        val startedAt = SystemClock.elapsedRealtime()
        _state.update {
            LocationUiState(current = it.current, tracking = true)
        }
        trackingJob = repository.fixes(precise)
            .onEach { fix -> onFix(fix) }
            .catch { _state.update { it.copy(tracking = false, message = LocationMessage.Error) } }
            .launchIn(viewModelScope)
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val elapsed = (SystemClock.elapsedRealtime() - startedAt) / 1_000
                _state.update { it.copy(elapsedSeconds = elapsed) }
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        timerJob?.cancel()
        trackingJob = null
        timerJob = null
        _state.update { it.copy(tracking = false) }
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
}
