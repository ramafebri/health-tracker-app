package com.rama.health.di

import com.rama.health.domain.repository.ReminderScheduler
import com.rama.health.service.reminder.ReminderAlarmScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {
    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: ReminderAlarmScheduler): ReminderScheduler
}
