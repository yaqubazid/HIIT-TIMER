package com.hiittimer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_presets")
data class WorkoutPreset(
    @PrimaryKey
    val title: String,
    val sets: Int,
    val getReadyTimeSeconds: Int,
    val workTimeSeconds: Int,
    val restTimeSeconds: Int,
    val coolDownTimeSeconds: Int
)
