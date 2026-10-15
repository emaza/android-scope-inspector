package com.example.scopeinspector.feature.location

import android.os.SystemClock
import kotlinx.coroutines.flow.Flow

/**
 * Origen de las lecturas de ubicación. La implementación real usa Play Services
 * ([LocationRepository]); en los tests se sustituye por un fake.
 *
 * Quien lo usa es responsable de haber comprobado antes que hay permiso de ubicación.
 */
interface LocationSource {
    /** Falso si el usuario tiene la ubicación del dispositivo desactivada en Ajustes. */
    fun isLocationEnabled(): Boolean

    /** Una sola lectura; null si el sistema no pudo obtener ninguna. */
    suspend fun currentFix(precise: Boolean): LocationFix?

    /** Lecturas continuas mientras se recoja el flujo. */
    fun fixes(precise: Boolean): Flow<LocationFix>
}

/** Reloj monótono, inyectable para que los tests controlen el tiempo transcurrido. */
fun interface ElapsedClock {
    fun elapsedMillis(): Long
}

object SystemElapsedClock : ElapsedClock {
    override fun elapsedMillis(): Long = SystemClock.elapsedRealtime()
}
