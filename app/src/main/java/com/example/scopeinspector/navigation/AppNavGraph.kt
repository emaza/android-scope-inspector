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
import com.example.scopeinspector.feature.inspector.InspectorScreen
import com.example.scopeinspector.feature.location.LocationScreen
import com.example.scopeinspector.shortcuts.ShortcutPolicy
import com.example.scopeinspector.ui.home.HomeScreen
import com.example.scopeinspector.ui.module.ModuleScreen

private const val HOME_ROUTE = "home"
const val INSPECTOR_ROUTE = "inspector"

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    // Con la app ya abierta, un atajo llega como nuevo intent (MainActivity es singleTask). Navigation no
    // lo procesa por sí sola, y `handleDeepLink` con las flags de un atajo intenta relanzar la tarea
    // en lugar de navegar. Se resuelve la ruta aquí: siempre queda Home debajo y no se duplica el módulo.
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity, navController) {
        val knownRoutes = DemoModule.entries.map { it.route }
        val listener = Consumer<Intent> { intent ->
            val route = ShortcutPolicy.moduleRoute(intent.dataString, knownRoutes) ?: return@Consumer
            navController.navigate(route) {
                popUpTo(HOME_ROUTE)
                launchSingleTop = true
            }
        }
        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }

    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            HomeScreen(
                onOpenModule = { module -> navController.navigate(module.route) },
                onOpenInspector = { navController.navigate(INSPECTOR_ROUTE) },
            )
        }
        composable(INSPECTOR_ROUTE) {
            InspectorScreen(onBack = { navController.popBackStack() })
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
