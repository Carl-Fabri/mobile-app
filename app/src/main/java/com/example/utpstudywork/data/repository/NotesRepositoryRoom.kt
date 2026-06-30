package com.example.utpstudywork.data.repository

import com.example.utpstudywork.data.local.dao.NoteDao
import com.example.utpstudywork.data.local.dao.TaskDao
import com.example.utpstudywork.data.local.entity.NoteEntity
import com.example.utpstudywork.data.local.entity.NoteWithTasks
import com.example.utpstudywork.data.local.entity.TaskEntity
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NotificationConfig
import com.example.utpstudywork.domain.model.NotificationType
import com.example.utpstudywork.domain.model.NoteStatus
import com.example.utpstudywork.domain.model.SessionType
import com.example.utpstudywork.domain.model.Task
import com.example.utpstudywork.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotesRepositoryRoom(
    private val noteDao: NoteDao,
    private val taskDao: TaskDao
) : NotesRepository {

    override fun observeNotes(): Flow<List<Note>> =
        noteDao.observeAllWithTasks().map { list -> list.map { it.toDomain() } }

    override fun observeFocusedNotes(): Flow<List<Note>> =
        noteDao.observeFocusedWithTasks().map { list -> list.map { it.toDomain() } }

    override fun observeActiveNotes(): Flow<List<Note>> =
        noteDao.observeActiveWithTasks().map { list -> list.map { it.toDomain() } }

    override fun observeByWeek(weekId: String): Flow<List<Note>> =
        noteDao.observeByWeek(weekId).map { list -> list.map { it.toDomain() } }

    override fun observeUnassigned(): Flow<List<Note>> =
        noteDao.observeUnassigned().map { list -> list.map { it.toDomainSimple() } }

    override fun observeByWeekAndDay(weekId: String, dayOfWeek: Int): Flow<List<Note>> =
        noteDao.observeByWeekAndDay(weekId, dayOfWeek).map { list -> list.map { it.toDomain() } }

    override fun observePendingNotes(): Flow<List<Note>> =
        noteDao.observePendingWithTasks().map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(note: Note) {
        noteDao.upsert(note.toEntity())
    }

    override suspend fun delete(id: String) {
        noteDao.delete(id)
    }

    override suspend fun setFocused(id: String, focused: Boolean) {
        noteDao.setFocused(id, focused)
    }

    override suspend fun setStatus(id: String, status: NoteStatus) {
        noteDao.setStatus(id, status.name)
    }

    override suspend fun assignToCalendar(noteId: String, weekId: String, dayOfWeek: Int) {
        noteDao.assignToCalendar(noteId, weekId, dayOfWeek)
    }

    override suspend fun removeFromCalendar(noteId: String) {
        noteDao.removeFromCalendar(noteId)
    }

    override suspend fun setObjective(noteId: String, objectiveId: String?) {
        noteDao.setObjective(noteId, objectiveId)
    }

    override suspend fun upsertTask(task: Task) {
        taskDao.upsert(task.toEntity())
    }

    override suspend fun deleteTask(id: String) {
        taskDao.delete(id)
    }

    override suspend fun setTaskDone(id: String, done: Boolean) {
        taskDao.setDone(id, done)
    }
}

// ----- Mappers -----

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

// For NoteEntity without tasks (e.g. observeUnassigned)
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

private fun TaskEntity.toDomain() = Task(
    id = id,
    noteId = noteId,
    text = text,
    isDone = isDone
)

private fun Note.toEntity() = NoteEntity(
    id = id,
    title = title,
    description = description,
    triggerAtMillis = triggerAtMillis,
    category = category.name,
    color = color,
    isFocused = isFocused,
    notifType = notification?.type?.name,
    notifCustomMessage = notification?.customMessage ?: "",
    notifFilterCategory = notification?.filterCategory?.name,
    status = status.name,
    objectiveId = objectiveId,
    calendarWeekId = calendarWeekId,
    calendarDayOfWeek = calendarDayOfWeek
)

private fun Task.toEntity() = TaskEntity(
    id = id,
    noteId = noteId,
    text = text,
    isDone = isDone
)
