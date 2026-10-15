package com.example.scopeinspector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.Modifier
import com.example.scopeinspector.navigation.AppNavGraph
import com.example.scopeinspector.ui.components.VersionFooter
import com.example.scopeinspector.ui.theme.ScopeInspectorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScopeInspectorTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    // El pie se ocupa de la barra de navegación: las pantallas no deben repetir ese margen.
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .consumeWindowInsets(WindowInsets.navigationBars),
                    ) {
                        AppNavGraph()
                    }
                    VersionFooter()
                }
            }
        }
    }
}
