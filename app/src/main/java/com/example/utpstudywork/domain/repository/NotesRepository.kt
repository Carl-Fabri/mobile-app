package com.example.utpstudywork.domain.repository

import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NoteStatus
import com.example.utpstudywork.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun observeNotes(): Flow<List<Note>>
    fun observeFocusedNotes(): Flow<List<Note>>
    suspend fun upsert(note: Note)
    suspend fun delete(id: String)
    suspend fun setFocused(id: String, focused: Boolean)
    suspend fun upsertTask(task: Task)
    suspend fun deleteTask(id: String)
    suspend fun setTaskDone(id: String, done: Boolean)
    suspend fun setStatus(id: String, status: NoteStatus)
    fun observeActiveNotes(): Flow<List<Note>>
    fun observeByWeek(weekId: String): Flow<List<Note>>
    fun observeUnassigned(): Flow<List<Note>>
    fun observeByWeekAndDay(weekId: String, dayOfWeek: Int): Flow<List<Note>>
    fun observePendingNotes(): Flow<List<Note>>
    suspend fun assignToCalendar(noteId: String, weekId: String, dayOfWeek: Int)
    suspend fun removeFromCalendar(noteId: String)
    suspend fun setObjective(noteId: String, objectiveId: String?)
}
