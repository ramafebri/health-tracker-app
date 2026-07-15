package com.rama.health.ui.medication.list

import com.rama.health.domain.model.MedicationReminder

data class MedicationListUiState(
    val medications: List<MedicationReminder> = emptyList(),
    val isLoading: Boolean = true,
    val medicationPendingDelete: MedicationReminder? = null,
)
