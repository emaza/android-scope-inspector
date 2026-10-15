package com.example.scopeinspector.feature.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.scopeinspector.R
import com.example.scopeinspector.navigation.DemoModule
import com.example.scopeinspector.permissions.AppPermission
import com.example.scopeinspector.permissions.PermissionDialogs
import com.example.scopeinspector.permissions.PermissionGroupState
import com.example.scopeinspector.permissions.PermissionState
import com.example.scopeinspector.permissions.rememberPermissionGroup
import com.example.scopeinspector.ui.components.PermissionStatusList
import kotlin.math.roundToInt

private val LocationPermissions = listOf(AppPermission.FineLocation, AppPermission.CoarseLocation)

private enum class LocationAction { LocateOnce, StartTracking }

private fun PermissionGroupState.hasPreciseAccess() =
    states[AppPermission.FineLocation] == PermissionState.Granted

private fun PermissionGroupState.hasLocationAccess() =
    hasPreciseAccess() || states[AppPermission.CoarseLocation] == PermissionState.Granted

/**
 * Demo real del permiso de ubicación: el mapa te sitúa con la posición obtenida y, al iniciar
 * el rastreo, dibuja la ruta que recorres. El permiso se pide al pulsar un botón, no al abrir
 * la pantalla. El mapa se recibe como parámetro ([mapContent]) para poder sustituirlo en los tests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = viewModel(factory = LocationViewModel.factory(LocalContext.current)),
    mapContent: @Composable (current: RoutePoint?, route: List<RoutePoint>, modifier: Modifier) -> Unit =
        { current, route, mapModifier -> RouteMap(current = current, route = route, modifier = mapModifier) },
) {
    val module = DemoModule.Location
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingAction by remember { mutableStateOf<LocationAction?>(null) }

    fun perform(action: LocationAction, precise: Boolean) {
        when (action) {
            LocationAction.LocateOnce -> viewModel.locateOnce(precise)
            LocationAction.StartTracking -> viewModel.startTracking(precise)
        }
    }

    val group = rememberPermissionGroup(LocationPermissions) { result ->
        val action = pendingAction
        pendingAction = null
        if (action != null && result.hasLocationAccess()) {
            perform(action, result.hasPreciseAccess())
        }
    }

    fun requestAnd(action: LocationAction) {
        if (group.hasLocationAccess()) {
            perform(action, group.hasPreciseAccess())
        } else {
            pendingAction = action
            group.request()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(module.titleRes)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            mapContent(
                state.current?.point,
                state.route,
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(12.dp)),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { requestAnd(LocationAction.LocateOnce) },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_locate_now))
                }
                if (state.tracking) {
                    OutlinedButton(onClick = viewModel::stopTracking, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_stop_tracking))
                    }
                } else {
                    Button(
                        onClick = { requestAnd(LocationAction.StartTracking) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.action_start_tracking))
                    }
                }
            }

            StatusMessage(state = state, hasAccess = group.hasLocationAccess())
            Readings(state = state, group = group)

            Text(text = stringResource(R.string.permissions_header), style = MaterialTheme.typography.titleMedium)
            PermissionStatusList(states = group.states)
        }
    }

    PermissionDialogs(
        group = group,
        title = stringResource(module.titleRes),
        rationale = stringResource(module.rationaleRes),
    )
}

@Composable
private fun StatusMessage(state: LocationUiState, hasAccess: Boolean) {
    val textRes = when {
        state.message != null -> state.message.textRes()
        !hasAccess -> R.string.msg_no_permission
        state.current == null -> R.string.location_no_fix_hint
        else -> null
    }
    if (textRes != null) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Readings(state: LocationUiState, group: PermissionGroupState) {
    val fix = state.current
    val none = stringResource(R.string.no_value)
    val accessRes = when {
        group.hasPreciseAccess() -> R.string.access_precise
        group.hasLocationAccess() -> R.string.access_approximate
        else -> R.string.access_none
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ReadingRow(R.string.reading_position, fix?.let { RouteMath.formatCoordinates(it.point) } ?: none)
        ReadingRow(
            R.string.reading_accuracy,
            fix?.let { stringResource(R.string.accuracy_value, it.accuracyMeters.roundToInt()) } ?: none,
        )
        ReadingRow(R.string.reading_access, stringResource(accessRes))
        ReadingRow(
            R.string.reading_speed,
            fix?.speedMetersPerSecond?.let { RouteMath.formatSpeedKmh(it) } ?: none,
        )
        ReadingRow(R.string.reading_distance, RouteMath.formatDistance(state.distanceMeters))
        ReadingRow(R.string.reading_time, RouteMath.formatDuration(state.elapsedSeconds))
        ReadingRow(R.string.reading_points, state.route.size.toString())
    }
}

@Composable
private fun ReadingRow(labelRes: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = stringResource(labelRes), style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun LocationMessage.textRes(): Int = when (this) {
    LocationMessage.LocationDisabled -> R.string.msg_location_disabled
    LocationMessage.NoFix -> R.string.msg_no_fix
    LocationMessage.Error -> R.string.msg_error
    LocationMessage.LowAccuracy -> R.string.msg_low_accuracy
}
