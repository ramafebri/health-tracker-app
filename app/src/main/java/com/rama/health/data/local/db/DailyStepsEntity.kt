package com.rama.health.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey val date: String, // ISO-8601 yyyy-MM-dd
    val steps: Int,
)
