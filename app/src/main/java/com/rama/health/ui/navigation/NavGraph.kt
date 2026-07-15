package com.rama.health.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rama.health.ui.dashboard.DashboardScreen
import com.rama.health.ui.history.HistoryScreen
import com.rama.health.ui.medication.edit.MedicationEditScreen
import com.rama.health.ui.medication.list.MedicationListScreen
import com.rama.health.ui.reminders.RemindersHubScreen
import com.rama.health.ui.water.WaterReminderScreen
import com.rama.health.ui.workout.WorkoutTrackingServiceEffect
import com.rama.health.ui.workout.active.ActiveWorkoutScreen
import com.rama.health.ui.workout.detail.WorkoutDetailScreen
import com.rama.health.ui.workout.list.WorkoutListScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    reminderDeepLink: ReminderDeepLink? = null,
    onReminderDeepLinkHandled: () -> Unit = {},
) {
    LaunchedEffect(reminderDeepLink) {
        when (val link = reminderDeepLink) {
            ReminderDeepLink.Water -> {
                navController.navigate(NavRoutes.WATER_REMINDER) {
                    launchSingleTop = true
                }
                onReminderDeepLinkHandled()
            }

            is ReminderDeepLink.Medication -> {
                navController.navigate(NavRoutes.medicationEdit(link.medicationId)) {
                    launchSingleTop = true
                }
                onReminderDeepLinkHandled()
            }

            null -> Unit
        }
    }

    WorkoutTrackingServiceEffect()
    NavHost(navController = navController, startDestination = NavRoutes.DASHBOARD) {
        composable(NavRoutes.DASHBOARD) {
            DashboardScreen(
                onNavigateToHistory = { navController.navigate(NavRoutes.HISTORY) },
                onNavigateToWorkouts = { navController.navigate(NavRoutes.WORKOUT_LIST) },
                onNavigateToReminders = { navController.navigate(NavRoutes.REMINDERS_HUB) },
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
        composable(NavRoutes.REMINDERS_HUB) {
            RemindersHubScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWaterReminder = { navController.navigate(NavRoutes.WATER_REMINDER) },
                onNavigateToMedicationList = { navController.navigate(NavRoutes.MEDICATION_LIST) },
            )
        }
        composable(NavRoutes.WATER_REMINDER) {
            WaterReminderScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(NavRoutes.MEDICATION_LIST) {
            MedicationListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { medicationId ->
                    navController.navigate(NavRoutes.medicationEdit(medicationId))
                },
            )
        }
        composable(
            route = NavRoutes.MEDICATION_EDIT,
            arguments = listOf(navArgument(NavRoutes.MEDICATION_ID_ARG) { type = NavType.StringType }),
        ) {
            MedicationEditScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
