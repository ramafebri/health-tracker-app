package com.rama.health.ui.navigation

object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val WORKOUT_LIST = "workout_list"
    const val ACTIVE_WORKOUT = "active_workout"
    const val WORKOUT_DETAIL = "workout_detail/{workoutId}"

    fun workoutDetail(workoutId: String): String = "workout_detail/$workoutId"

    const val WORKOUT_ID_ARG = "workoutId"
}
