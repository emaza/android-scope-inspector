package com.example.scopeinspector.permissions

/** Qué hay que hacer cuando el usuario pide un conjunto de permisos. */
sealed interface PermissionRequestPlan {
    /** Nada que pedir: todo concedido o no aplica en esta versión de Android. */
    data object NoAction : PermissionRequestPlan

    /** Los que faltan están denegados de forma permanente: solo se pueden activar desde Ajustes. */
    data object OpenSettings : PermissionRequestPlan

    /** Se denegó antes y Android aconseja explicar el motivo antes de volver a pedirlo. */
    data class ShowRationale(val permissions: List<AppPermission>) : PermissionRequestPlan

    /** Se puede pedir ya con el diálogo del sistema. */
    data class Launch(val permissions: List<AppPermission>) : PermissionRequestPlan
}

/**
 * Decide qué hacer con los permisos [permissions] según su estado actual. Es la lógica pura de
 * `PermissionGroupState.request()`, separada de Android para poder probarla.
 *
 * Solo se piden los que están en [PermissionState.Denied]; los denegados de forma permanente no se
 * pueden volver a pedir, y si son lo único que falta se envía al usuario a Ajustes. Si se pide
 * alguno y Android sugiere un rationale para cualquiera de ellos, se muestra antes de pedir.
 */
fun planPermissionRequest(
    permissions: List<AppPermission>,
    states: Map<AppPermission, PermissionState>,
    shouldShowRationale: (AppPermission) -> Boolean,
): PermissionRequestPlan {
    val requestable = permissions.filter { states[it] == PermissionState.Denied }
    val blocked = permissions.any { states[it] == PermissionState.PermanentlyDenied }
    return when {
        requestable.isEmpty() -> if (blocked) PermissionRequestPlan.OpenSettings else PermissionRequestPlan.NoAction
        requestable.any(shouldShowRationale) -> PermissionRequestPlan.ShowRationale(requestable)
        else -> PermissionRequestPlan.Launch(requestable)
    }
}
