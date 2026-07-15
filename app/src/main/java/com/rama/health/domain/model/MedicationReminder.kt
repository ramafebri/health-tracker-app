package com.rama.health.domain.model

data class MedicationReminder(
    val id: String,
    val name: String,
    val dosage: String? = null,
    val enabled: Boolean = true,
    val repeatDays: RepeatDays = RepeatDays.allDays(),
    val times: List<MedicationTime> = emptyList(),
    val createdAtMillis: Long = 0L,
)
