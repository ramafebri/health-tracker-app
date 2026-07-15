package com.rama.health.ui.water

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.WaterReminderSettings
import com.rama.health.domain.model.WaterScheduleMode
import com.rama.health.domain.usecase.LogWaterIntakeUseCase
import com.rama.health.domain.usecase.ObserveWaterReminderSettingsUseCase
import com.rama.health.domain.usecase.UpdateWaterReminderSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WaterReminderViewModel @Inject constructor(
    observeWaterReminderSettings: ObserveWaterReminderSettingsUseCase,
    private val updateWaterReminderSettings: UpdateWaterReminderSettingsUseCase,
    private val logWaterIntake: LogWaterIntakeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WaterReminderUiState(isLoading = true))
    val uiState: StateFlow<WaterReminderUiState> = _uiState.asStateFlow()
    private var persistJob: Job? = null

    init {
        viewModelScope.launch {
            observeWaterReminderSettings().collect { settings ->
                _uiState.update { current ->
                    if (current.isLoading) {
                        settings.toUiState()
                    } else {
                        current.copy(todayIntakeMl = settings.todayIntakeMl)
                    }
                }
            }
        }
    }

    fun onEnabledChanged(enabled: Boolean) {
        applyChangeAndPersist { it.copy(enabled = enabled, validationError = null) }
    }

    fun onScheduleModeChanged(mode: WaterScheduleMode) {
        applyChangeAndPersist { it.copy(scheduleMode = mode, validationError = null) }
    }

    fun onIntervalChanged(minutes: Int) {
        applyChangeAndPersist { it.copy(intervalMinutes = minutes, validationError = null) }
    }

    fun onActiveStartChanged(minutes: Int) {
        applyChangeAndPersist { it.copy(activeStartMinutes = minutes, validationError = null) }
    }

    fun onActiveEndChanged(minutes: Int) {
        applyChangeAndPersist { it.copy(activeEndMinutes = minutes, validationError = null) }
    }

    fun onFixedTimesChanged(times: List<MedicationTime>) {
        applyChangeAndPersist { it.copy(fixedTimes = times, validationError = null) }
    }

    fun onDailyGoalInputChanged(value: String) {
        updateLocal { it.copy(dailyGoalInput = value, validationError = null) }
    }

    fun onDailyGoalSaved() {
        applyChangeAndPersist { it.copy(validationError = null) }
    }

    fun onLogWater() {
        viewModelScope.launch {
            logWaterIntake()
        }
    }

    private fun updateLocal(transform: (WaterReminderUiState) -> WaterReminderUiState) {
        _uiState.update { current ->
            transform(current.copy(isLoading = false))
        }
    }

    private fun applyChangeAndPersist(
        transform: (WaterReminderUiState) -> WaterReminderUiState,
    ) {
        val current = _uiState.value
        val candidate = transform(current.copy(isLoading = false))
        val validationError = validate(candidate)
        if (validationError != null) {
            _uiState.update { it.copy(validationError = validationError, isSaving = false) }
            return
        }
        _uiState.value = candidate.copy(validationError = null)
        schedulePersist()
    }

    private fun schedulePersist() {
        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            val latest = _uiState.value
            val validationError = validate(latest)
            if (validationError != null) {
                _uiState.update { it.copy(validationError = validationError, isSaving = false) }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, validationError = null) }
            updateWaterReminderSettings(latest.toSettings())
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun validate(state: WaterReminderUiState): WaterReminderValidationError? {
        if (state.enabled) {
            when (state.scheduleMode) {
                WaterScheduleMode.INTERVAL -> {
                    if (state.intervalMinutes < 15) {
                        return WaterReminderValidationError.INTERVAL_INVALID
                    }
                    if (state.activeEndMinutes <= state.activeStartMinutes) {
                        return WaterReminderValidationError.ACTIVE_HOURS_INVALID
                    }
                }

                WaterScheduleMode.FIXED_TIMES -> {
                    if (state.fixedTimes.isEmpty()) {
                        return WaterReminderValidationError.FIXED_TIMES_EMPTY
                    }
                }
            }
        }

        val goalInput = state.dailyGoalInput.trim()
        if (goalInput.isNotEmpty() && (goalInput.toIntOrNull() == null || goalInput.toInt() <= 0)) {
            return WaterReminderValidationError.DAILY_GOAL_INVALID
        }

        return null
    }

    private fun WaterReminderSettings.toUiState(): WaterReminderUiState = WaterReminderUiState(
        enabled = enabled,
        scheduleMode = scheduleMode,
        intervalMinutes = intervalMinutes,
        activeStartMinutes = activeStartMinutes,
        activeEndMinutes = activeEndMinutes,
        fixedTimes = fixedTimes,
        dailyGoalInput = dailyGoalMl?.toString().orEmpty(),
        todayIntakeMl = todayIntakeMl,
        isLoading = false,
    )

    private fun WaterReminderUiState.toSettings(): WaterReminderSettings {
        val goalInput = dailyGoalInput.trim()
        return WaterReminderSettings(
            enabled = enabled,
            scheduleMode = scheduleMode,
            intervalMinutes = intervalMinutes,
            activeStartMinutes = activeStartMinutes,
            activeEndMinutes = activeEndMinutes,
            fixedTimes = fixedTimes,
            dailyGoalMl = goalInput.toIntOrNull(),
            todayIntakeMl = todayIntakeMl,
        )
    }

    private companion object {
        const val PERSIST_DEBOUNCE_MS = 300L
    }
}
