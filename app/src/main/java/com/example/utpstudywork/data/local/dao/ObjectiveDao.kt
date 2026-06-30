package com.example.utpstudywork.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.utpstudywork.data.local.entity.ObjectiveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ObjectiveDao {

    @Query("SELECT * FROM objectives WHERE weekId = :weekId ORDER BY orderIndex")
    fun observeByWeek(weekId: String): Flow<List<ObjectiveEntity>>

    @Upsert
    suspend fun upsert(entity: ObjectiveEntity)

    @Query("DELETE FROM objectives WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE objectives SET isDone = :done WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean)

    @Query("UPDATE objectives SET weekId = :weekId WHERE id = :id")
    suspend fun moveToWeek(id: String, weekId: String)
}
