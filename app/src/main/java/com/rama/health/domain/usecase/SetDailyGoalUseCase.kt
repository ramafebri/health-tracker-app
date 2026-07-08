package com.rama.health.domain.usecase

import com.rama.health.domain.repository.StepRepository
import javax.inject.Inject

class SetDailyGoalUseCase @Inject constructor(
    private val repository: StepRepository,
) {
    suspend operator fun invoke(goal: Int) = repository.setDailyGoal(goal)
}
