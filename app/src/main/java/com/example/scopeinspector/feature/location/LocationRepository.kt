package com.example.scopeinspector.feature.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Acceso a FusedLocationProviderClient. Quien lo llama es responsable de haber comprobado
 * antes que hay permiso de ubicación (fina o aproximada).
 */
class LocationRepository(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /** Falso si el usuario tiene la ubicación del dispositivo desactivada en Ajustes. */
    fun isLocationEnabled(): Boolean = LocationManagerCompat.isLocationEnabled(locationManager)

    /** Una sola lectura; null si el sistema no pudo obtener ninguna. */
    @SuppressLint("MissingPermission")
    suspend fun currentFix(precise: Boolean): LocationFix? = suspendCancellableCoroutine { cont ->
        val cancellation = CancellationTokenSource()
        cont.invokeOnCancellation { cancellation.cancel() }
        client.getCurrentLocation(priority(precise), cancellation.token)
            .addOnSuccessListener { location -> cont.resume(location?.toFix()) }
            .addOnFailureListener { error -> cont.resumeWithException(error) }
    }

    /** Lecturas continuas mientras se recoja el flujo. */
    @SuppressLint("MissingPermission")
    fun fixes(precise: Boolean): Flow<LocationFix> = callbackFlow {
        val request = LocationRequest.Builder(priority(precise), UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { trySend(it.toFix()) }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun priority(precise: Boolean): Int =
        if (precise) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY

    private fun Location.toFix() = LocationFix(
        point = RoutePoint(latitude, longitude),
        accuracyMeters = accuracy,
        speedMetersPerSecond = if (hasSpeed()) speed else null,
        timeMillis = time,
    )

    private companion object {
        const val UPDATE_INTERVAL_MS = 2_000L
        const val MIN_DISTANCE_METERS = 2f
    }
}
