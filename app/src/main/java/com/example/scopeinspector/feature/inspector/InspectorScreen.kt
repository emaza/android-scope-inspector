package com.example.scopeinspector.feature.inspector

import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import com.example.scopeinspector.R

/**
 * Inspector del dispositivo: el equivalente Android de Browser Scope Inspector. Enseña todo lo que
 * una app puede leer sin pedir permisos en ejecución, con datos reales de este móvil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorScreen(onBack: () -> Unit, modifier: Modifier = Modifier, source: DeviceInfoSource? = null) {
    val context = LocalContext.current
    val infoSource = source ?: remember(context) { AndroidDeviceInfoSource(context) }
    var snapshot by remember(infoSource) { mutableStateOf(infoSource.snapshot()) }
    val sections = remember(snapshot) { InspectorReport.sections(snapshot) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inspector_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
                },
                actions = {
                    TextButton(onClick = { snapshot = infoSource.snapshot() }) {
                        Text(stringResource(R.string.action_refresh))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PrivacyBanner() }
            items(sections, key = { it.id }) { section -> SectionCard(section) }
            item { LiveSensorsCard() }
            item { ClipboardCard() }
        }
    }
}

@Composable
private fun PrivacyBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.inspector_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.inspector_local_only),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun SectionCard(section: InspectorSection) {
    InspectorCard(title = stringResource(section.titleRes), why = stringResource(section.whyRes)) {
        section.rows.forEach { row -> DataRow(label = stringResource(row.labelRes), value = row.value.text()) }
    }
}

@Composable
private fun InspectorCard(title: String, why: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
            HorizontalDivider()
            Text(
                text = why,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            modifier = Modifier.weight(0.42f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.58f),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun RowValue.text(): String = when (this) {
    is RowValue.Plain -> text
    is RowValue.Localized -> stringResource(res, *args.toTypedArray())
}

@Composable
private fun LiveSensorsCard() {
    val context = LocalContext.current
    val available = remember(context) { availableLiveSensors(context.getSystemService<SensorManager>()) }
    val readings = rememberLiveSensorReadings()
    InspectorCard(
        title = stringResource(R.string.inspector_sensors_live_title),
        why = stringResource(R.string.inspector_sensors_live_why),
    ) {
        if (available.isEmpty()) {
            Text(stringResource(R.string.inspector_sensors_none), style = MaterialTheme.typography.bodyMedium)
        }
        available.forEach { sensor ->
            DataRow(
                label = stringResource(sensor.labelRes),
                value = readings[sensor]?.let { sensor.format(it) } ?: stringResource(R.string.inspector_waiting),
            )
        }
    }
}

@Composable
private fun ClipboardCard() {
    val context = LocalContext.current
    var described by remember { mutableStateOf(ClipboardProbe.describe(context)) }
    var read by remember { mutableStateOf<ClipboardReading.Read?>(null) }
    InspectorCard(
        title = stringResource(R.string.inspector_clipboard_title),
        why = stringResource(R.string.inspector_clipboard_why),
    ) {
        DataRow(
            label = stringResource(R.string.inspector_clipboard_has_clip),
            value = if (described.hasClip) {
                stringResource(R.string.inspector_clipboard_types, described.mimeTypes.joinToString().ifEmpty { "?" })
            } else {
                stringResource(R.string.inspector_no)
            },
        )
        read?.let { reading ->
            DataRow(
                label = stringResource(R.string.inspector_clipboard_content),
                value = reading.text ?: stringResource(R.string.inspector_clipboard_empty),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                described = ClipboardProbe.describe(context)
                read = ClipboardProbe.read(context)
            }) { Text(stringResource(R.string.action_read_clipboard)) }
            if (read != null) {
                OutlinedButton(onClick = { read = null }) { Text(stringResource(R.string.action_hide)) }
            }
        }
    }
}
