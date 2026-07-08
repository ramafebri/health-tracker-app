package com.rama.health.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rama.health.domain.usecase.ObserveStepHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    observeStepHistory: ObserveStepHistoryUseCase,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = observeStepHistory()
        .map { records -> HistoryUiState(records = records, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(records = emptyList(), isLoading = true),
        )
}
