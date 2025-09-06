package com.hiittimer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPresetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: WorkoutPreset)

    @Query("SELECT * FROM workout_presets ORDER BY title ASC")
    fun getAllPresets(): Flow<List<WorkoutPreset>>

    @Query("SELECT * FROM workout_presets WHERE title = :title LIMIT 1")
    suspend fun getPresetByTitle(title: String): WorkoutPreset?

    @Query("DELETE FROM workout_presets WHERE title = :title")
    suspend fun deletePreset(title: String)

    // You can add more queries here as needed, for example:
    // @Update
    // suspend fun updatePreset(preset: WorkoutPreset)
}
