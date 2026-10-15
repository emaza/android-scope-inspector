package com.example.scopeinspector.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.scopeinspector.R
import com.example.scopeinspector.permissions.AppPermission
import com.example.scopeinspector.permissions.PermissionState
import com.example.scopeinspector.permissions.labelRes

/** Una línea por permiso con su estado; el estado se indica con texto, no solo con color. */
@Composable
fun PermissionStatusList(
    states: Map<AppPermission, PermissionState>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        states.forEach { (permission, state) ->
            Text(
                text = stringResource(
                    R.string.permission_row,
                    stringResource(permission.labelRes),
                    stringResource(state.labelRes()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = state.color(),
            )
        }
    }
}

@Composable
private fun PermissionState.color(): Color = when (this) {
    PermissionState.Granted -> MaterialTheme.colorScheme.primary
    PermissionState.Denied, PermissionState.PermanentlyDenied -> MaterialTheme.colorScheme.error
    PermissionState.NotApplicable -> MaterialTheme.colorScheme.onSurfaceVariant
}
