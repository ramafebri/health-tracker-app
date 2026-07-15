package com.rama.health.ui.medication.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.RepeatDays
import com.rama.health.domain.usecase.ObserveMedicationReminderUseCase
import com.rama.health.domain.usecase.SaveMedicationReminderUseCase
import com.rama.health.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MedicationEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeMedicationReminder: ObserveMedicationReminderUseCase,
    private val saveMedicationReminder: SaveMedicationReminderUseCase,
) : ViewModel() {

    private val medicationId: String = checkNotNull(savedStateHandle[NavRoutes.MEDICATION_ID_ARG])
    private val isCreateMode = medicationId == NavRoutes.NEW_MEDICATION_ID

    private val _uiState = MutableStateFlow(
        MedicationEditUiState(
            isCreateMode = isCreateMode,
            isLoading = !isCreateMode,
        ),
    )
    val uiState: StateFlow<MedicationEditUiState> = _uiState.asStateFlow()

    init {
        if (!isCreateMode) {
            viewModelScope.launch {
                observeMedicationReminder(medicationId).collect { reminder ->
                    if (reminder != null) {
                        _uiState.update { current ->
                            if (current.isLoading) {
                                reminder.toUiState()
                            } else {
                                current
                            }
                        }
                    }
                }
            }
        }
    }

    fun onNameChanged(value: String) {
        _uiState.update { it.copy(name = value, validationError = null) }
    }

    fun onDosageChanged(value: String) {
        _uiState.update { it.copy(dosage = value) }
    }

    fun onEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(enabled = enabled) }
    }

    fun onRepeatDaysChanged(repeatDays: RepeatDays) {
        _uiState.update { it.copy(repeatDays = repeatDays, validationError = null) }
    }

    fun onTimesChanged(times: List<MedicationTime>) {
        _uiState.update { it.copy(times = times, validationError = null) }
    }

    fun toggleDay(day: DayOfWeek) {
        val current = _uiState.value.repeatDays
        val bit = 1 shl (day.value - 1)
        val updated = if (current.mask and bit != 0) {
            RepeatDays(current.mask and bit.inv())
        } else {
            RepeatDays(current.mask or bit)
        }
        onRepeatDaysChanged(updated)
    }

    fun onSave() {
        val current = _uiState.value
        val validationError = validate(current)
        if (validationError != null) {
            _uiState.update { it.copy(validationError = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, validationError = null) }
            val id = if (isCreateMode) UUID.randomUUID().toString() else medicationId
            saveMedicationReminder(
                MedicationReminder(
                    id = id,
                    name = current.name.trim(),
                    dosage = current.dosage.trim().ifBlank { null },
                    enabled = current.enabled,
                    repeatDays = current.repeatDays,
                    times = current.times.sortedWith(compareBy({ it.hour }, { it.minute })),
                    createdAtMillis = if (isCreateMode) System.currentTimeMillis() else current.createdAtMillis,
                ),
            )
            _uiState.update { it.copy(isSaving = false, saveSucceeded = true) }
        }
    }

    fun onSaveHandled() {
        _uiState.update { it.copy(saveSucceeded = false) }
    }

    private fun validate(state: MedicationEditUiState): MedicationEditValidationError? {
        if (state.name.isBlank()) return MedicationEditValidationError.NAME_REQUIRED
        if (state.times.isEmpty()) return MedicationEditValidationError.TIMES_REQUIRED
        if (state.repeatDays.mask == 0) return MedicationEditValidationError.REPEAT_DAYS_REQUIRED
        val uniqueTimes = state.times.map { it.hour * 60 + it.minute }.toSet()
        if (uniqueTimes.size != state.times.size) return MedicationEditValidationError.DUPLICATE_TIME
        return null
    }

    private fun MedicationReminder.toUiState(): MedicationEditUiState = MedicationEditUiState(
        isCreateMode = false,
        name = name,
        dosage = dosage.orEmpty(),
        enabled = enabled,
        repeatDays = repeatDays,
        times = times,
        isLoading = false,
        createdAtMillis = createdAtMillis,
    )
}
