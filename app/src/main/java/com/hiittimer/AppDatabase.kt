package com.hiittimer

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WorkoutPreset::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutPresetDao(): WorkoutPresetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hiit_timer_database"
                )
                // Wipes and rebuilds instead of migrating if no Migration object.
                // Migration is not covered in this basic example.
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
