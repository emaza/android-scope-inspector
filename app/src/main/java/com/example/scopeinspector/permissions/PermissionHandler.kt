package com.example.scopeinspector.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/** Diálogo que la pantalla debe mostrar ahora mismo para el grupo de permisos. */
enum class PermissionDialog {
    None,
    Rationale,
    Settings,
}

internal fun readPermissionStates(
    context: Context,
    activity: Activity?,
    store: RequestedPermissionsStore,
    permissions: List<AppPermission>,
): Map<AppPermission, PermissionState> = permissions.associateWith { permission ->
    resolvePermissionState(
        applicable = permission.isApplicable(),
        granted = ContextCompat.checkSelfPermission(context, permission.manifestName) ==
            PackageManager.PERMISSION_GRANTED,
        requestedBefore = store.wasRequested(permission),
        shouldShowRationale = activity?.shouldShowRequestPermissionRationale(permission.manifestName) == true,
    )
}

/**
 * Gestiona la solicitud de un conjunto de permisos de un módulo: pide los que faltan,
 * muestra un rationale tras una denegación y ofrece ir a Ajustes si la denegación es
 * permanente. Nunca bloquea: si el usuario deniega, la pantalla sigue funcionando
 * en modo reducido.
 */
@Stable
class PermissionGroupState internal constructor(
    val permissions: List<AppPermission>,
    private val context: Context,
    private val activity: Activity?,
) {
    private val store = RequestedPermissionsStore(context)
    private var pendingRequest: List<AppPermission> = emptyList()

    internal var launch: (Array<String>) -> Unit = {}

    var states: Map<AppPermission, PermissionState> by mutableStateOf(
        readPermissionStates(context, activity, store, permissions),
    )
        private set

    var dialog: PermissionDialog by mutableStateOf(PermissionDialog.None)
        private set

    val status: PermissionState
        get() = aggregatePermissionState(states.values)

    fun refresh() {
        states = readPermissionStates(context, activity, store, permissions)
    }

    fun request() {
        refresh()
        val plan = planPermissionRequest(permissions, states) { permission ->
            activity?.shouldShowRequestPermissionRationale(permission.manifestName) == true
        }
        when (plan) {
            PermissionRequestPlan.NoAction -> Unit

            PermissionRequestPlan.OpenSettings -> dialog = PermissionDialog.Settings

            is PermissionRequestPlan.ShowRationale -> {
                pendingRequest = plan.permissions
                dialog = PermissionDialog.Rationale
            }

            is PermissionRequestPlan.Launch -> launchRequest(plan.permissions)
        }
    }

    fun confirmRationale() {
        dialog = PermissionDialog.None
        launchRequest(pendingRequest)
    }

    fun dismissDialog() {
        dialog = PermissionDialog.None
    }

    fun openSettings() {
        dialog = PermissionDialog.None
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun launchRequest(requested: List<AppPermission>) {
        if (requested.isEmpty()) return
        store.markRequested(requested)
        launch(requested.map { it.manifestName }.toTypedArray())
    }
}

@Composable
fun rememberPermissionGroup(
    permissions: List<AppPermission>,
    onResult: (PermissionGroupState) -> Unit = {},
): PermissionGroupState {
    val context = LocalContext.current.applicationContext
    val activity = LocalActivity.current
    val group = remember(permissions, activity) { PermissionGroupState(permissions, context, activity) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        group.refresh()
        onResult(group)
    }
    SideEffect { group.launch = { names -> launcher.launch(names) } }
    // Al volver de Ajustes el usuario pudo cambiar el permiso.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { group.refresh() }
    return group
}

/** Solo lectura: estado actual de los permisos, refrescado al volver a la pantalla. */
@Composable
fun rememberPermissionStates(permissions: List<AppPermission>): Map<AppPermission, PermissionState> {
    val context = LocalContext.current.applicationContext
    val activity = LocalActivity.current
    val store = remember { RequestedPermissionsStore(context) }
    var states by remember(permissions, activity) {
        mutableStateOf(readPermissionStates(context, activity, store, permissions))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        states = readPermissionStates(context, activity, store, permissions)
    }
    return states
}
