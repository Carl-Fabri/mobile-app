package com.example.utpstudywork.domain.repository

import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.Objective
import kotlinx.coroutines.flow.Flow

interface PlannerRepository {
    // ── Semanas ───────────────────────────────────────────────────────────────
    suspend fun ensureWeek(weekId: String, startMillis: Long)

    // ── Objetivos ─────────────────────────────────────────────────────────────
    fun observeObjectivesForWeek(weekId: String): Flow<List<Objective>>
    suspend fun upsertObjective(objective: Objective)
    suspend fun deleteObjective(id: String)
    suspend fun setObjectiveDone(id: String, done: Boolean)
    suspend fun moveObjectiveToWeek(objectiveId: String, newWeekId: String)

    // ── Notas en el calendario ────────────────────────────────────────────────
    fun observeNotesForWeek(weekId: String): Flow<List<Note>>
    fun observeUnassignedNotes(): Flow<List<Note>>
    suspend fun assignNoteToDay(noteId: String, weekId: String, dayOfWeek: Int, objectiveId: String?)
    suspend fun removeNoteFromCalendar(noteId: String)
    suspend fun setNoteObjective(noteId: String, objectiveId: String?)
}
