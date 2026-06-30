package com.example.utpstudywork.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.utpstudywork.data.local.dao.NoteDao
import com.example.utpstudywork.data.local.dao.ObjectiveDao
import com.example.utpstudywork.data.local.dao.TaskDao
import com.example.utpstudywork.data.local.dao.WeekDao
import com.example.utpstudywork.data.local.dao.WordFrequencyDao
import com.example.utpstudywork.data.local.entity.NoteEntity
import com.example.utpstudywork.data.local.entity.ObjectiveEntity
import com.example.utpstudywork.data.local.entity.TaskEntity
import com.example.utpstudywork.data.local.entity.WeekEntity
import com.example.utpstudywork.data.local.entity.WordFrequencyEntity

@Database(
    entities = [
        NoteEntity::class, TaskEntity::class, WordFrequencyEntity::class,
        WeekEntity::class, ObjectiveEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun wordFrequencyDao(): WordFrequencyDao
    abstract fun weekDao(): WeekDao
    abstract fun objectiveDao(): ObjectiveDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studywork.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
