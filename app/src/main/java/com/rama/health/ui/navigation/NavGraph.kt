package com.rama.health.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rama.health.ui.dashboard.DashboardScreen
import com.rama.health.ui.history.HistoryScreen
import com.rama.health.ui.workout.WorkoutTrackingServiceEffect
import com.rama.health.ui.workout.active.ActiveWorkoutScreen
import com.rama.health.ui.workout.detail.WorkoutDetailScreen
import com.rama.health.ui.workout.list.WorkoutListScreen

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    WorkoutTrackingServiceEffect()
    NavHost(navController = navController, startDestination = NavRoutes.DASHBOARD) {
        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(
                onNavigateToHistory = { navController.navigate(NavRoutes.HISTORY) },
                onNavigateToWorkouts = { navController.navigate(NavRoutes.WORKOUT_LIST) },
            )
        }
        composable(NavRoutes.HISTORY) {
            HistoryScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(NavRoutes.WORKOUT_LIST) {
            WorkoutListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToActiveWorkout = { navController.navigate(NavRoutes.ACTIVE_WORKOUT) },
                onNavigateToDetail = { workoutId ->
                    navController.navigate(NavRoutes.workoutDetail(workoutId))
                },
            )
        }
        composable(NavRoutes.ACTIVE_WORKOUT) {
            ActiveWorkoutScreen(
                onNavigateBack = { navController.popBackStack() },
                onWorkoutStopped = { workoutId ->
                    if (workoutId != null) {
                        navController.navigate(NavRoutes.workoutDetail(workoutId)) {
                            popUpTo(NavRoutes.WORKOUT_LIST) { inclusive = false }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable(
            route = NavRoutes.WORKOUT_DETAIL,
            arguments = listOf(navArgument(NavRoutes.WORKOUT_ID_ARG) { type = NavType.StringType }),
        ) {
            WorkoutDetailScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
