package com.example.utpstudywork.data.repository

import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NoteStatus
import com.example.utpstudywork.domain.model.Task
import com.example.utpstudywork.domain.repository.NotesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class NotesRepositoryInMemory : NotesRepository {
    private val notes = MutableStateFlow<List<Note>>(emptyList())

    override fun observeNotes() = notes.asStateFlow()

    override fun observeFocusedNotes(): Flow<List<Note>> =
        notes.map { it.filter { n -> n.isFocused } }

    override suspend fun upsert(note: Note) {
        notes.update { list -> list.filter { it.id != note.id } + note }
    }

    override suspend fun delete(id: String) {
        notes.update { it.filterNot { n -> n.id == id } }
    }

    override suspend fun setFocused(id: String, focused: Boolean) {
        notes.update { list -> list.map { if (it.id == id) it.copy(isFocused = focused) else it } }
    }

    override suspend fun upsertTask(task: Task) {
        notes.update { list ->
            list.map { note ->
                if (note.id == task.noteId) {
                    note.copy(tasks = note.tasks.filter { it.id != task.id } + task)
                } else note
            }
        }
    }

    override suspend fun deleteTask(id: String) {
        notes.update { list ->
            list.map { note -> note.copy(tasks = note.tasks.filterNot { it.id == id }) }
        }
    }

    override suspend fun setTaskDone(id: String, done: Boolean) {
        notes.update { list ->
            list.map { note ->
                note.copy(tasks = note.tasks.map { if (it.id == id) it.copy(isDone = done) else it })
            }
        }
    }

    override suspend fun setStatus(id: String, status: NoteStatus) {
        notes.update { list -> list.map { if (it.id == id) it.copy(status = status) else it } }
    }

    override fun observeActiveNotes(): Flow<List<Note>> =
        notes.map { it.filter { n -> n.status == NoteStatus.ACTIVE } }

    override fun observeByWeek(weekId: String): Flow<List<Note>> =
        notes.map { it.filter { n -> n.calendarWeekId == weekId } }

    override fun observeUnassigned(): Flow<List<Note>> =
        notes.map { it.filter { n -> n.calendarWeekId == null } }

    override fun observeByWeekAndDay(weekId: String, dayOfWeek: Int): Flow<List<Note>> =
        notes.map { it.filter { n -> n.calendarWeekId == weekId && n.calendarDayOfWeek == dayOfWeek } }

    override fun observePendingNotes(): Flow<List<Note>> =
        notes.map { it.filter { n -> n.status == NoteStatus.PENDING } }

    override suspend fun assignToCalendar(noteId: String, weekId: String, dayOfWeek: Int) {
        notes.update { list ->
            list.map { if (it.id == noteId) it.copy(calendarWeekId = weekId, calendarDayOfWeek = dayOfWeek) else it }
        }
    }

    override suspend fun removeFromCalendar(noteId: String) {
        notes.update { list ->
            list.map { if (it.id == noteId) it.copy(calendarWeekId = null, calendarDayOfWeek = null, objectiveId = null) else it }
        }
    }

    override suspend fun setObjective(noteId: String, objectiveId: String?) {
        notes.update { list ->
            list.map { if (it.id == noteId) it.copy(objectiveId = objectiveId) else it }
        }
    }
}
