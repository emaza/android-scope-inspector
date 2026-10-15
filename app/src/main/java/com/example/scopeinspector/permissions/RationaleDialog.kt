package com.example.scopeinspector.permissions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.scopeinspector.R

@Composable
fun RationaleDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Dibuja el diálogo que corresponda al estado de [group] (rationale o ir a Ajustes). */
@Composable
fun PermissionDialogs(group: PermissionGroupState, title: String, rationale: String) {
    when (group.dialog) {
        PermissionDialog.None -> Unit

        PermissionDialog.Rationale -> RationaleDialog(
            title = title,
            message = rationale,
            confirmText = stringResource(R.string.action_continue),
            onConfirm = group::confirmRationale,
            onDismiss = group::dismissDialog,
        )

        PermissionDialog.Settings -> RationaleDialog(
            title = title,
            message = stringResource(R.string.settings_dialog_message),
            confirmText = stringResource(R.string.action_open_settings),
            onConfirm = group::openSettings,
            onDismiss = group::dismissDialog,
        )
    }
}
