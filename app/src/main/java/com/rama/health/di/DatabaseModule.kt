package com.rama.health.di

import android.content.Context
import androidx.room.Room
import com.rama.health.data.local.db.AppDatabase
import com.rama.health.data.local.db.DailyStepsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "health_tracker.db").build()

    @Provides
    fun provideDailyStepsDao(database: AppDatabase): DailyStepsDao = database.dailyStepsDao()
}
