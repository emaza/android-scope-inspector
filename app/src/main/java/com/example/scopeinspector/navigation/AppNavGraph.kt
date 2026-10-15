package com.example.scopeinspector.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.scopeinspector.feature.location.LocationScreen
import com.example.scopeinspector.ui.home.HomeScreen
import com.example.scopeinspector.ui.module.ModuleScreen

private const val HOME_ROUTE = "home"

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = HOME_ROUTE) {
        composable(HOME_ROUTE) {
            HomeScreen(onOpenModule = { module -> navController.navigate(module.route) })
        }
        DemoModule.entries.forEach { module ->
            composable(module.route) {
                val onBack = { navController.popBackStack() }
                when (module) {
                    DemoModule.Location -> LocationScreen(onBack = { onBack() })
                    else -> ModuleScreen(module = module, onBack = { onBack() })
                }
            }
        }
    }
}
