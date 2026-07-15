package com.rama.health.ui.navigation

sealed class ReminderDeepLink {
    data object Water : ReminderDeepLink()
    data class Medication(val medicationId: String) : ReminderDeepLink()
}
