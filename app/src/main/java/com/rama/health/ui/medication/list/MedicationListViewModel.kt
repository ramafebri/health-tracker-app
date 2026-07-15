package com.rama.health.ui.medication.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.usecase.DeleteMedicationReminderUseCase
import com.rama.health.domain.usecase.ObserveMedicationRemindersUseCase
import com.rama.health.domain.usecase.ToggleMedicationReminderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationListViewModel @Inject constructor(
    observeMedicationReminders: ObserveMedicationRemindersUseCase,
    private val toggleMedicationReminder: ToggleMedicationReminderUseCase,
    private val deleteMedicationReminder: DeleteMedicationReminderUseCase,
) : ViewModel() {

    private val localState = MutableStateFlow(MedicationListUiState())

    val uiState: StateFlow<MedicationListUiState> = combine(
        observeMedicationReminders(),
        localState,
    ) { medications, local ->
        local.copy(
            medications = medications,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MedicationListUiState(isLoading = true),
    )

    fun onToggleEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            toggleMedicationReminder(id, enabled)
        }
    }

    fun onDeleteRequested(medication: MedicationReminder) {
        localState.update { it.copy(medicationPendingDelete = medication) }
    }

    fun onDeleteDismissed() {
        localState.update { it.copy(medicationPendingDelete = null) }
    }

    fun onDeleteConfirmed() {
        val medication = localState.value.medicationPendingDelete ?: return
        viewModelScope.launch {
            deleteMedicationReminder(medication.id)
            localState.update { it.copy(medicationPendingDelete = null) }
        }
    }
}
