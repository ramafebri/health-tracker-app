package com.rama.health.ui.medication.edit

import androidx.lifecycle.SavedStateHandle
import com.rama.health.MainDispatcherRule
import com.rama.health.domain.model.MedicationReminder
import com.rama.health.domain.model.MedicationTime
import com.rama.health.domain.model.RepeatDays
import com.rama.health.domain.usecase.ObserveMedicationReminderUseCase
import com.rama.health.domain.usecase.SaveMedicationReminderUseCase
import com.rama.health.ui.navigation.NavRoutes
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MedicationEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val saveMedicationReminder = mockk<SaveMedicationReminderUseCase>()
    private val observeMedicationReminder = mockk<ObserveMedicationReminderUseCase>()

    private fun createViewModel(
        medicationId: String = NavRoutes.NEW_MEDICATION_ID,
    ): MedicationEditViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf(NavRoutes.MEDICATION_ID_ARG to medicationId),
        )
        return MedicationEditViewModel(
            savedStateHandle,
            observeMedicationReminder,
            saveMedicationReminder,
        )
    }

    @Test
    fun onSave_blankName_setsNameRequiredValidationError() = runTest {
        val vm = createViewModel()
        vm.onNameChanged("")
        vm.onTimesChanged(listOf(MedicationTime(8, 0)))

        vm.onSave()

        assertEquals(MedicationEditValidationError.NAME_REQUIRED, vm.uiState.value.validationError)
    }

    @Test
    fun onSave_emptyTimes_setsTimesRequiredValidationError() = runTest {
        val vm = createViewModel()
        vm.onNameChanged("Aspirin")
        vm.onTimesChanged(emptyList())

        vm.onSave()

        assertEquals(MedicationEditValidationError.TIMES_REQUIRED, vm.uiState.value.validationError)
    }

    @Test
    fun onSave_emptyRepeatDays_setsRepeatDaysRequiredValidationError() = runTest {
        val vm = createViewModel()
        vm.onNameChanged("Aspirin")
        vm.onTimesChanged(listOf(MedicationTime(8, 0)))
        vm.onRepeatDaysChanged(RepeatDays.none())

        vm.onSave()

        assertEquals(
            MedicationEditValidationError.REPEAT_DAYS_REQUIRED,
            vm.uiState.value.validationError,
        )
    }

    @Test
    fun onSave_duplicateTimes_setsDuplicateTimeValidationError() = runTest {
        val vm = createViewModel()
        vm.onNameChanged("Aspirin")
        vm.onTimesChanged(listOf(MedicationTime(8, 0), MedicationTime(8, 0)))

        vm.onSave()

        assertEquals(MedicationEditValidationError.DUPLICATE_TIME, vm.uiState.value.validationError)
    }

    @Test
    fun onSave_validState_invokesSaveUseCase() = runTest {
        coEvery { saveMedicationReminder(any()) } just Runs

        val vm = createViewModel()
        vm.onNameChanged("  Aspirin  ")
        vm.onDosageChanged(" 100mg ")
        vm.onTimesChanged(listOf(MedicationTime(20, 0), MedicationTime(8, 0)))
        vm.onRepeatDaysChanged(RepeatDays.allDays())

        vm.onSave()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        val saved = slot<MedicationReminder>()
        coVerify { saveMedicationReminder(capture(saved)) }
        assertEquals("Aspirin", saved.captured.name)
        assertEquals("100mg", saved.captured.dosage)
        assertEquals(listOf(MedicationTime(8, 0), MedicationTime(20, 0)), saved.captured.times)
        assertTrue(vm.uiState.value.saveSucceeded)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun createMode_startsWithEmptyFormAndNotLoading() = runTest {
        val vm = createViewModel()

        assertTrue(vm.uiState.value.isCreateMode)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("", vm.uiState.value.name)
        assertNull(vm.uiState.value.validationError)
    }

    @Test
    fun editMode_loadsExistingReminder() = runTest {
        val existing = MedicationReminder(
            id = "med-42",
            name = "Ibuprofen",
            dosage = "200mg",
            enabled = false,
            repeatDays = RepeatDays.fromSetOfDays(setOf(java.time.DayOfWeek.MONDAY)),
            times = listOf(MedicationTime(9, 30)),
            createdAtMillis = 5_000L,
        )
        every { observeMedicationReminder("med-42") } returns flowOf(existing)

        val vm = createViewModel(medicationId = "med-42")
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.uiState.value.isCreateMode)
        assertEquals("Ibuprofen", vm.uiState.value.name)
        assertEquals("200mg", vm.uiState.value.dosage)
        assertFalse(vm.uiState.value.enabled)
        assertFalse(vm.uiState.value.isLoading)
    }
}
