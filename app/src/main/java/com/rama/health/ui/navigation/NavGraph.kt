package com.rama.health.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rama.health.ui.dashboard.DashboardScreen
import com.rama.health.ui.history.HistoryScreen

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = NavRoutes.DASHBOARD) {
        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(onNavigateToHistory = { navController.navigate(NavRoutes.HISTORY) })
        }
        composable(NavRoutes.HISTORY) {
            HistoryScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
