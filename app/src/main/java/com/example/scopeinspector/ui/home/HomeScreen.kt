package com.example.scopeinspector.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.scopeinspector.R
import com.example.scopeinspector.navigation.DemoModule
import com.example.scopeinspector.permissions.rememberPermissionStates
import com.example.scopeinspector.ui.components.ModuleDescription
import com.example.scopeinspector.ui.components.PermissionStatusList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenModule: (DemoModule) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.home_intro),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            items(DemoModule.entries) { module ->
                ModuleCard(module = module, onClick = { onOpenModule(module) })
            }
            item { ShortcutsHint() }
        }
    }
}

/** Explica dónde se ven los atajos: solo salen en el icono real de la app, no en el acceso directo fijado. */
@Composable
private fun ShortcutsHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.shortcuts_hint_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.shortcuts_hint_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ModuleCard(module: DemoModule, onClick: () -> Unit) {
    val states = rememberPermissionStates(module.permissions)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = stringResource(module.titleRes), style = MaterialTheme.typography.titleMedium)
            ModuleDescription(module = module)
            PermissionStatusList(states = states)
        }
    }
}
