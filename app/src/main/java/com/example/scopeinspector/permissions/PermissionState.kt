package com.example.scopeinspector.permissions

import androidx.annotation.StringRes
import com.example.scopeinspector.R

enum class PermissionState {
    Granted,
    Denied,
    PermanentlyDenied,
    NotApplicable,
}

/**
 * Android no informa de la denegación permanente: se deduce de que el permiso ya se
 * pidió alguna vez, sigue sin concederse y el sistema ya no sugiere mostrar un rationale.
 */
fun resolvePermissionState(
    applicable: Boolean,
    granted: Boolean,
    requestedBefore: Boolean,
    shouldShowRationale: Boolean,
): PermissionState = when {
    !applicable -> PermissionState.NotApplicable
    granted -> PermissionState.Granted
    requestedBefore && !shouldShowRationale -> PermissionState.PermanentlyDenied
    else -> PermissionState.Denied
}

/** Estado conjunto de un módulo; ignora los permisos que no aplican a esta versión de Android. */
fun aggregatePermissionState(states: Collection<PermissionState>): PermissionState {
    val relevant = states.filter { it != PermissionState.NotApplicable }
    return when {
        relevant.isEmpty() -> PermissionState.NotApplicable
        relevant.all { it == PermissionState.Granted } -> PermissionState.Granted
        relevant.any { it == PermissionState.PermanentlyDenied } -> PermissionState.PermanentlyDenied
        else -> PermissionState.Denied
    }
}

@StringRes
fun PermissionState.labelRes(): Int = when (this) {
    PermissionState.Granted -> R.string.state_granted
    PermissionState.Denied -> R.string.state_denied
    PermissionState.PermanentlyDenied -> R.string.state_permanently_denied
    PermissionState.NotApplicable -> R.string.state_not_applicable
}
