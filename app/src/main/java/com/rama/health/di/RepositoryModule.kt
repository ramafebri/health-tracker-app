package com.rama.health.di

import com.rama.health.data.repository.MedicationReminderRepositoryImpl
import com.rama.health.data.repository.StepRepositoryImpl
import com.rama.health.data.repository.WaterReminderRepositoryImpl
import com.rama.health.data.repository.WorkoutRepositoryImpl
import com.rama.health.domain.repository.MedicationReminderRepository
import com.rama.health.domain.repository.StepRepository
import com.rama.health.domain.repository.WaterReminderRepository
import com.rama.health.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindStepRepository(impl: StepRepositoryImpl): StepRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository

    @Binds
    @Singleton
    abstract fun bindWaterReminderRepository(impl: WaterReminderRepositoryImpl): WaterReminderRepository

    @Binds
    @Singleton
    abstract fun bindMedicationReminderRepository(
        impl: MedicationReminderRepositoryImpl,
    ): MedicationReminderRepository
}
