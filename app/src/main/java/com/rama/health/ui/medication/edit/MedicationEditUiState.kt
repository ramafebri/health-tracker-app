package com.rama.health.ui.medication.edit

enum class MedicationEditValidationError {
    NAME_REQUIRED,
    TIMES_REQUIRED,
    REPEAT_DAYS_REQUIRED,
    DUPLICATE_TIME,
}

data class MedicationEditUiState(
    val isCreateMode: Boolean = true,
    val name: String = "",
    val dosage: String = "",
    val enabled: Boolean = true,
    val repeatDays: com.rama.health.domain.model.RepeatDays =
        com.rama.health.domain.model.RepeatDays.allDays(),
    val times: List<com.rama.health.domain.model.MedicationTime> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val validationError: MedicationEditValidationError? = null,
    val saveSucceeded: Boolean = false,
    val createdAtMillis: Long = 0L,
)
