package com.rama.health.di

import com.rama.health.data.location.FusedLocationDataSource
import com.rama.health.data.location.LocationDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds
    @Singleton
    abstract fun bindLocationDataSource(impl: FusedLocationDataSource): LocationDataSource
}
