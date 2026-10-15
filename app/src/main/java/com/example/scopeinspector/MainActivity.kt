package com.example.scopeinspector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.scopeinspector.navigation.AppNavGraph
import com.example.scopeinspector.ui.theme.ScopeInspectorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScopeInspectorTheme {
                AppNavGraph()
            }
        }
    }
}
