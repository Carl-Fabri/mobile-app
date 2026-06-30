package com.example.utpstudywork.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.utpstudywork.data.local.entity.NoteEntity
import com.example.utpstudywork.data.local.entity.NoteWithTasks
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("SELECT * FROM notes ORDER BY triggerAtMillis DESC")
    fun observeAllWithTasks(): Flow<List<NoteWithTasks>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isFocused = 1 ORDER BY triggerAtMillis DESC")
    fun observeFocusedWithTasks(): Flow<List<NoteWithTasks>>

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE notes SET isFocused = :focused WHERE id = :id")
    suspend fun setFocused(id: String, focused: Boolean)

    @Transaction
    @Query("SELECT * FROM notes WHERE status = 'ACTIVE' ORDER BY triggerAtMillis DESC")
    fun observeActiveWithTasks(): Flow<List<NoteWithTasks>>

    @Query("UPDATE notes SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Transaction
    @Query("SELECT * FROM notes WHERE calendarWeekId = :weekId AND status != 'COMPLETED' ORDER BY calendarDayOfWeek, objectiveId NULLS LAST")
    fun observeByWeek(weekId: String): Flow<List<NoteWithTasks>>

    @Query("SELECT * FROM notes WHERE calendarWeekId IS NULL AND status != 'COMPLETED' ORDER BY triggerAtMillis DESC")
    fun observeUnassigned(): Flow<List<NoteEntity>>

    @Query("UPDATE notes SET calendarWeekId = :weekId, calendarDayOfWeek = :dayOfWeek WHERE id = :id")
    suspend fun assignToCalendar(id: String, weekId: String, dayOfWeek: Int)

    @Query("UPDATE notes SET calendarWeekId = NULL, calendarDayOfWeek = NULL, objectiveId = NULL WHERE id = :id")
    suspend fun removeFromCalendar(id: String)

    @Query("UPDATE notes SET objectiveId = :objectiveId WHERE id = :id")
    suspend fun setObjective(id: String, objectiveId: String?)

    @Transaction
    @Query("SELECT * FROM notes WHERE calendarWeekId = :weekId AND calendarDayOfWeek = :dayOfWeek ORDER BY status DESC, triggerAtMillis DESC")
    fun observeByWeekAndDay(weekId: String, dayOfWeek: Int): Flow<List<NoteWithTasks>>

    @Transaction
    @Query("SELECT * FROM notes WHERE status = 'PENDING' ORDER BY triggerAtMillis DESC")
    fun observePendingWithTasks(): Flow<List<NoteWithTasks>>
}
