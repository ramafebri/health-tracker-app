package com.rama.health.domain.model

import java.time.LocalDate

data class DailyStepRecord(
    val date: LocalDate,
    val steps: Int,
)
