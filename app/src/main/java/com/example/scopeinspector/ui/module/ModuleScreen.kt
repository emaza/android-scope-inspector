package com.example.scopeinspector.ui.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.scopeinspector.R
import com.example.scopeinspector.navigation.DemoModule
import com.example.scopeinspector.permissions.PermissionDialogs
import com.example.scopeinspector.permissions.PermissionState
import com.example.scopeinspector.permissions.rememberPermissionGroup
import com.example.scopeinspector.ui.components.ModuleDescription
import com.example.scopeinspector.ui.components.PermissionStatusList

/**
 * Pantalla genérica de módulo de la Fase 1: muestra el estado de sus permisos y permite
 * probar el flujo de solicitud. Cada módulo la sustituirá por su pantalla real en su fase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleScreen(module: DemoModule, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val group = rememberPermissionGroup(module.permissions)
    val status = group.status

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
            Text(text = stringResource(module.summaryRes), style = MaterialTheme.typography.bodyLarge)
            ModuleDescription(module = module, showHowToTest = true)
            Text(text = stringResource(R.string.permissions_header), style = MaterialTheme.typography.titleMedium)
            PermissionStatusList(states = group.states)
            Text(text = stringResource(status.noteRes()), style = MaterialTheme.typography.bodyMedium)
            if (status == PermissionState.Denied || status == PermissionState.PermanentlyDenied) {
                Button(onClick = group::request) {
                    Text(stringResource(R.string.action_request_permissions))
                }
            }
        }
    }

    PermissionDialogs(
        group = group,
        title = stringResource(module.titleRes),
        rationale = stringResource(module.rationaleRes),
    )
}

private fun PermissionState.noteRes(): Int = when (this) {
    PermissionState.Granted -> R.string.note_granted
    PermissionState.Denied, PermissionState.PermanentlyDenied -> R.string.note_denied
    PermissionState.NotApplicable -> R.string.note_not_applicable
}
