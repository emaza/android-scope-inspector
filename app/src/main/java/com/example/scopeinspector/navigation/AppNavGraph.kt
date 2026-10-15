package com.example.scopeinspector.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.util.Consumer
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.scopeinspector.feature.location.LocationScreen
import com.example.scopeinspector.shortcuts.ShortcutPolicy
import com.example.scopeinspector.ui.home.HomeScreen
import com.example.scopeinspector.ui.module.ModuleScreen

private const val HOME_ROUTE = "home"

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    // Con la app ya abierta, un atajo llega como nuevo intent (MainActivity es singleTask) y
    // Navigation no lo procesa por sí sola: hay que pasárselo.
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity, navController) {
        val listener = Consumer<Intent> { intent -> navController.handleDeepLink(intent) }
        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }

    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            HomeScreen(onOpenModule = { module -> navController.navigate(module.route) })
        }
        DemoModule.entries.forEach { module ->
            composable(
                route = module.route,
                deepLinks = listOf(navDeepLink { uriPattern = ShortcutPolicy.moduleUri(module.route) }),
            ) {
                val onBack = { navController.popBackStack() }
                when (module) {
                    DemoModule.Location -> LocationScreen(onBack = { onBack() })
                    else -> ModuleScreen(module = module, onBack = { onBack() })
                }
            }
        }
    }
}
