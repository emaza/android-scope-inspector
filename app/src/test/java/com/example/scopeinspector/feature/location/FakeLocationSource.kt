package com.example.scopeinspector.feature.location

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Fake de [LocationSource] controlable desde el test: qué responde, cuándo y cuántas veces se usa. */
class FakeLocationSource : LocationSource {
    var enabled = true

    /** Respuesta de [currentFix]; si [currentFixError] no es null, se lanza en su lugar. */
    var currentFixResult: LocationFix? = null
    var currentFixError: Exception? = null

    /** Mientras no se complete, [currentFix] queda esperando (para probar el estado de carga). */
    var currentFixGate: CompletableDeferred<Unit>? = null

    /** Si no es null, el flujo de [fixes] falla con esta excepción nada más empezar. */
    var fixesError: Exception? = null

    /** Lecturas continuas: el test las emite con `fixesFlow.emit(...)`. */
    val fixesFlow = MutableSharedFlow<LocationFix>(extraBufferCapacity = 16)

    var currentFixCalls = 0
        private set
    var fixesSubscriptions = 0
        private set

    /** Valor de `precise` de cada petición (una puntual o una suscripción continua). */
    val requestedPrecise = mutableListOf<Boolean>()

    override fun isLocationEnabled(): Boolean = enabled

    override suspend fun currentFix(precise: Boolean): LocationFix? {
        currentFixCalls++
        requestedPrecise += precise
        currentFixGate?.await()
        currentFixError?.let { throw it }
        return currentFixResult
    }

    override fun fixes(precise: Boolean): Flow<LocationFix> {
        requestedPrecise += precise
        return flow {
            fixesSubscriptions++
            fixesError?.let { throw it }
            emitAll(fixesFlow)
        }
    }
}

/** Reloj controlable. */
class FakeElapsedClock(var nowMillis: Long = 0L) : ElapsedClock {
    override fun elapsedMillis(): Long = nowMillis
}

/** Lectura de ejemplo: por defecto precisa (5 m) y en Madrid; `latitudeOffset` la desplaza al norte. */
fun fix(latitudeOffset: Double = 0.0, accuracy: Float = 5f, speed: Float? = null) = LocationFix(
    point = RoutePoint(40.0 + latitudeOffset, -3.0),
    accuracyMeters = accuracy,
    speedMetersPerSecond = speed,
    timeMillis = 0L,
)
