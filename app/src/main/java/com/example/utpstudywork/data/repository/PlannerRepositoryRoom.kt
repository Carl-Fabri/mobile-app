package com.example.utpstudywork.data.repository

import com.example.utpstudywork.data.local.dao.NoteDao
import com.example.utpstudywork.data.local.dao.ObjectiveDao
import com.example.utpstudywork.data.local.dao.WeekDao
import com.example.utpstudywork.data.local.entity.NoteEntity
import com.example.utpstudywork.data.local.entity.NoteWithTasks
import com.example.utpstudywork.data.local.entity.ObjectiveEntity
import com.example.utpstudywork.data.local.entity.TaskEntity
import com.example.utpstudywork.data.local.entity.WeekEntity
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NotificationConfig
import com.example.utpstudywork.domain.model.NotificationType
import com.example.utpstudywork.domain.model.NoteStatus
import com.example.utpstudywork.domain.model.Objective
import com.example.utpstudywork.domain.model.SessionType
import com.example.utpstudywork.domain.model.Task
import com.example.utpstudywork.domain.repository.PlannerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PlannerRepositoryRoom(
    private val weekDao: WeekDao,
    private val objectiveDao: ObjectiveDao,
    private val noteDao: NoteDao
) : PlannerRepository {

    // ── Semanas ───────────────────────────────────────────────────────────────

    override suspend fun ensureWeek(weekId: String, startMillis: Long) {
        if (weekDao.get(weekId) == null) {
            weekDao.upsert(WeekEntity(id = weekId, startMillis = startMillis))
        }
    }

    // ── Objetivos ─────────────────────────────────────────────────────────────

    override fun observeObjectivesForWeek(weekId: String): Flow<List<Objective>> =
        combine(
            objectiveDao.observeByWeek(weekId),
            noteDao.observeByWeek(weekId)
        ) { objectives, notesWithTasks ->
            val notes = notesWithTasks.map { it.toDomain() }
            objectives.map { entity ->
                val objNotes = notes.filter { it.objectiveId == entity.id }
                Objective(
                    id = entity.id,
                    weekId = entity.weekId,
                    title = entity.title,
                    orderIndex = entity.orderIndex,
                    isDone = entity.isDone,
                    notes = objNotes
                )
            }
        }

    override suspend fun upsertObjective(objective: Objective) =
        objectiveDao.upsert(objective.toEntity())

    override suspend fun deleteObjective(id: String) = objectiveDao.delete(id)

    override suspend fun setObjectiveDone(id: String, done: Boolean) =
        objectiveDao.setDone(id, done)

    override suspend fun moveObjectiveToWeek(objectiveId: String, newWeekId: String) =
        objectiveDao.moveToWeek(objectiveId, newWeekId)

    // ── Notas en el calendario ────────────────────────────────────────────────

    override fun observeNotesForWeek(weekId: String): Flow<List<Note>> =
        noteDao.observeByWeek(weekId).map { list -> list.map { it.toDomain() } }

    override fun observeUnassignedNotes(): Flow<List<Note>> =
        noteDao.observeUnassigned().map { list -> list.map { it.toDomainSimple() } }

    override suspend fun assignNoteToDay(noteId: String, weekId: String, dayOfWeek: Int, objectiveId: String?) {
        noteDao.assignToCalendar(noteId, weekId, dayOfWeek)
        if (objectiveId != null) {
            noteDao.setObjective(noteId, objectiveId)
        }
    }

    override suspend fun removeNoteFromCalendar(noteId: String) {
        noteDao.removeFromCalendar(noteId)
    }

    override suspend fun setNoteObjective(noteId: String, objectiveId: String?) {
        noteDao.setObjective(noteId, objectiveId)
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun NoteWithTasks.toDomain(): Note {
    val notif = note.notifType?.let { typeStr ->
        NotificationConfig(
            type = NotificationType.valueOf(typeStr),
            customMessage = note.notifCustomMessage,
            filterCategory = note.notifFilterCategory?.let { SessionType.valueOf(it) }
        )
    }
    return Note(
        id = note.id,
        title = note.title,
        description = note.description,
        triggerAtMillis = note.triggerAtMillis,
        category = SessionType.valueOf(note.category),
        color = note.color,
        tasks = tasks.map { it.toDomain() },
        notification = notif,
        isFocused = note.isFocused,
        status = NoteStatus.valueOf(note.status),
        objectiveId = note.objectiveId,
        calendarWeekId = note.calendarWeekId,
        calendarDayOfWeek = note.calendarDayOfWeek
    )
}

private fun NoteEntity.toDomainSimple(): Note {
    val notif = notifType?.let { typeStr ->
        NotificationConfig(
            type = NotificationType.valueOf(typeStr),
            customMessage = notifCustomMessage,
            filterCategory = notifFilterCategory?.let { SessionType.valueOf(it) }
        )
    }
    return Note(
        id = id,
        title = title,
        description = description,
        triggerAtMillis = triggerAtMillis,
        category = SessionType.valueOf(category),
        color = color,
        tasks = emptyList(),
        notification = notif,
        isFocused = isFocused,
        status = NoteStatus.valueOf(status),
        objectiveId = objectiveId,
        calendarWeekId = calendarWeekId,
        calendarDayOfWeek = calendarDayOfWeek
    )
}

private fun TaskEntity.toDomain() = Task(id = id, noteId = noteId, text = text, isDone = isDone)

private fun Objective.toEntity() = ObjectiveEntity(
    id = id, weekId = weekId, title = title, orderIndex = orderIndex, isDone = isDone
)
