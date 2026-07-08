package com.rama.health.ui.history

import com.rama.health.domain.model.DailyStepRecord

data class HistoryUiState(
    val records: List<DailyStepRecord> = emptyList(),
    val isLoading: Boolean = true,
)
