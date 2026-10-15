package com.example.scopeinspector.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.scopeinspector.R
import com.example.scopeinspector.navigation.DemoModule

/**
 * Ficha de un módulo: qué aporta y un ejemplo concreto; opcionalmente, cómo comprobar
 * que funciona (se muestra en la pantalla del módulo, no en la tarjeta de inicio).
 */
@Composable
fun ModuleDescription(
    module: DemoModule,
    modifier: Modifier = Modifier,
    showHowToTest: Boolean = false,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabeledText(R.string.label_feature, module.featureRes)
        LabeledText(R.string.label_example, module.exampleRes)
        if (showHowToTest) {
            LabeledText(R.string.label_how_to_test, module.howToTestRes)
        }
    }
}

@Composable
private fun LabeledText(@StringRes labelRes: Int, @StringRes textRes: Int) {
    Column {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = stringResource(textRes), style = MaterialTheme.typography.bodyMedium)
    }
}
