package com.rama.health.ui.navigation

object NavRoutes {
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val WORKOUT_LIST = "workout_list"
    const val ACTIVE_WORKOUT = "active_workout"
    const val WORKOUT_DETAIL = "workout_detail/{workoutId}"

    fun workoutDetail(workoutId: String): String = "workout_detail/$workoutId"

    const val WORKOUT_ID_ARG = "workoutId"

    const val REMINDERS_HUB = "reminders_hub"
    const val WATER_REMINDER = "water_reminder"
    const val MEDICATION_LIST = "medication_list"
    const val MEDICATION_EDIT = "medication_edit/{medicationId}"

    fun medicationEdit(medicationId: String): String = "medication_edit/$medicationId"

    const val MEDICATION_ID_ARG = "medicationId"
    const val NEW_MEDICATION_ID = "new"
}
