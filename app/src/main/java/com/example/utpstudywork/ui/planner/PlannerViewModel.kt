package com.example.utpstudywork.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.utpstudywork.data.di.UseCases
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NoteStatus
import com.example.utpstudywork.domain.model.Objective
import com.example.utpstudywork.domain.model.SessionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.UUID

// ── Diálogos ─────────────────────────────────────────────────────────────────

sealed class PlannerDialog {
    object None : PlannerDialog()
    data class NewObjective(val editingId: String? = null, val title: String = "") : PlannerDialog()
    data class AssignNote(val dayOfWeek: Int, val objectiveId: String? = null) : PlannerDialog()
    data class CreateAndAssign(
        val dayOfWeek: Int,
        val objectiveId: String? = null,
        val title: String = "",
        val description: String = "",
        val category: SessionType = SessionType.WORK
    ) : PlannerDialog()
    data class AssignToObjective(val note: Note) : PlannerDialog()
}

// ── Estado ────────────────────────────────────────────────────────────────────

data class PlannerUiState(
    val weekId: String = "",
    val weekLabel: String = "",
    val weekStartMillis: Long = 0L,
    val objectives: List<Objective> = emptyList(),
    val looseNotes: List<Note> = emptyList(),
    val unassignedNotes: List<Note> = emptyList(),
    val dialog: PlannerDialog = PlannerDialog.None,
    val categoryFilter: SessionType? = null    // null = mostrar todas las categorías
)

class PlannerViewModel(private val use: UseCases) : ViewModel() {

    private val _state = MutableStateFlow(
        PlannerUiState(
            weekStartMillis = currentWeekStartMillis(),
            weekId = weekIdFor(currentWeekStartMillis()),
            weekLabel = formatWeekLabel(currentWeekStartMillis())
        )
    )
    val state: StateFlow<PlannerUiState> = _state

    private val _weekStart = MutableStateFlow(currentWeekStartMillis())

    init {
        viewModelScope.launch {
            _weekStart.collectLatest { startMillis ->
                val weekId = weekIdFor(startMillis)
                use.plannerRepo.ensureWeek(weekId, startMillis)
                _state.update {
                    it.copy(
                        weekId = weekId,
                        weekStartMillis = startMillis,
                        weekLabel = formatWeekLabel(startMillis)
                    )
                }

                combine(
                    use.plannerRepo.observeObjectivesForWeek(weekId),
                    use.plannerRepo.observeNotesForWeek(weekId),
                    use.plannerRepo.observeUnassignedNotes()
                ) { objectives, allWeekNotes, unassigned ->
                    Triple(objectives, allWeekNotes, unassigned)
                }.collect { (objectives, allWeekNotes, unassigned) ->
                    // Loose notes = notes in this week without an objectiveId
                    val looseNotes = allWeekNotes.filter { it.objectiveId == null }
                    _state.update {
                        it.copy(
                            objectives = objectives,
                            looseNotes = looseNotes,
                            unassignedNotes = unassigned
                        )
                    }
                }
            }
        }
    }

    // ── Navegación semanal ────────────────────────────────────────────────────

    fun goToPreviousWeek() { _weekStart.value -= WEEK_MS }
    fun goToNextWeek() { _weekStart.value += WEEK_MS }
    fun goToCurrentWeek() { _weekStart.value = currentWeekStartMillis() }

    // ── Diálogos ──────────────────────────────────────────────────────────────

    fun openNewObjectiveDialog(objective: Objective? = null) {
        _state.update {
            it.copy(dialog = PlannerDialog.NewObjective(objective?.id, objective?.title ?: ""))
        }
    }

    fun openAssignNoteDialog(dayOfWeek: Int, objectiveId: String? = null) {
        _state.update { it.copy(dialog = PlannerDialog.AssignNote(dayOfWeek, objectiveId)) }
    }

    fun openCreateNoteDialog(dayOfWeek: Int, objectiveId: String? = null) {
        _state.update { it.copy(dialog = PlannerDialog.CreateAndAssign(dayOfWeek, objectiveId)) }
    }

    fun openAssignToObjectiveDialog(note: Note) {
        _state.update { it.copy(dialog = PlannerDialog.AssignToObjective(note)) }
    }

    fun closeDialog() { _state.update { it.copy(dialog = PlannerDialog.None) } }

    fun updateObjectiveDialogTitle(title: String) {
        _state.update {
            val d = it.dialog as? PlannerDialog.NewObjective ?: return@update it
            it.copy(dialog = d.copy(title = title))
        }
    }

    fun updateCreateNoteTitle(title: String) {
        _state.update {
            val d = it.dialog as? PlannerDialog.CreateAndAssign ?: return@update it
            it.copy(dialog = d.copy(title = title))
        }
    }

    fun updateCreateNoteDescription(description: String) {
        _state.update {
            val d = it.dialog as? PlannerDialog.CreateAndAssign ?: return@update it
            it.copy(dialog = d.copy(description = description))
        }
    }

    fun updateCreateNoteCategory(category: SessionType) {
        _state.update {
            val d = it.dialog as? PlannerDialog.CreateAndAssign ?: return@update it
            it.copy(dialog = d.copy(category = category))
        }
    }

    fun setCategoryFilter(category: SessionType?) {
        _state.update { it.copy(categoryFilter = category) }
    }

    // ── Objetivos ─────────────────────────────────────────────────────────────

    fun saveObjective() {
        val d = _state.value.dialog as? PlannerDialog.NewObjective ?: return
        val title = d.title.trim()
        if (title.isEmpty()) return
        val weekId = _state.value.weekId
        val obj = if (d.editingId != null) {
            _state.value.objectives.find { it.id == d.editingId }?.copy(title = title) ?: return
        } else {
            Objective(
                id = UUID.randomUUID().toString(),
                weekId = weekId,
                title = title,
                orderIndex = _state.value.objectives.size
            )
        }
        viewModelScope.launch { use.plannerRepo.upsertObjective(obj); closeDialog() }
    }

    fun deleteObjective(id: String) {
        viewModelScope.launch { use.plannerRepo.deleteObjective(id) }
    }

    fun toggleObjectiveDone(obj: Objective) {
        viewModelScope.launch { use.plannerRepo.setObjectiveDone(obj.id, !obj.isDone) }
    }

    // ── Notas en el calendario ────────────────────────────────────────────────

    fun assignNoteToDay(note: Note, dayOfWeek: Int, objectiveId: String? = null) {
        val weekId = _state.value.weekId
        viewModelScope.launch {
            use.plannerRepo.assignNoteToDay(note.id, weekId, dayOfWeek, objectiveId)
            // If today's day, activate the note and notify
            if (isToday(weekId, dayOfWeek)) {
                use.notesRepo.setStatus(note.id, NoteStatus.ACTIVE)
                use.notificationManager.notifyAssignedToToday(note)
            }
            closeDialog()
        }
    }

    fun createAndAssignNote() {
        val d = _state.value.dialog as? PlannerDialog.CreateAndAssign ?: return
        val title = d.title.trim()
        if (title.isEmpty()) return
        val weekId = _state.value.weekId
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = title,
            description = d.description.trim(),
            triggerAtMillis = System.currentTimeMillis(),
            category = d.category,
            objectiveId = d.objectiveId,
            calendarWeekId = weekId,
            calendarDayOfWeek = d.dayOfWeek
        )
        viewModelScope.launch {
            use.notesRepo.upsert(note)
            if (isToday(weekId, d.dayOfWeek)) {
                use.notesRepo.setStatus(note.id, NoteStatus.ACTIVE)
                use.notificationManager.notifyAssignedToToday(note)
            }
            closeDialog()
        }
    }

    fun removeNoteFromCalendar(noteId: String) {
        viewModelScope.launch { use.plannerRepo.removeNoteFromCalendar(noteId) }
    }

    fun toggleNoteDone(note: Note) {
        val newStatus = if (note.status == NoteStatus.COMPLETED) NoteStatus.ACTIVE else NoteStatus.COMPLETED
        viewModelScope.launch { use.notesRepo.setStatus(note.id, newStatus) }
    }

    fun assignNoteToObjective(note: Note, objective: Objective) {
        viewModelScope.launch {
            use.plannerRepo.setNoteObjective(note.id, objective.id)
            closeDialog()
        }
    }

    // ── Drag & Drop (objetivos entre semanas) ─────────────────────────────────

    fun moveObjectiveToAdjacentWeek(objectiveId: String, toNextWeek: Boolean) {
        viewModelScope.launch {
            val currentStart = _state.value.weekStartMillis
            val newStart = if (toNextWeek) currentStart + WEEK_MS else currentStart - WEEK_MS
            val newWeekId = weekIdFor(newStart)
            use.plannerRepo.ensureWeek(newWeekId, newStart)
            use.plannerRepo.moveObjectiveToWeek(objectiveId, newWeekId)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun isToday(weekId: String, dayOfWeek: Int): Boolean {
        val today = LocalDate.now()
        val currentWeekId = weekIdFor(currentWeekStartMillis())
        return weekId == currentWeekId && dayOfWeek == today.dayOfWeek.value
    }

    companion object {
        private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

        fun currentWeekStartMillis(): Long {
            val today = LocalDate.now()
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            return monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        fun weekIdFor(startMillis: Long): String =
            Instant.ofEpochMilli(startMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString()

        fun formatWeekLabel(startMillis: Long): String {
            val zone = ZoneId.systemDefault()
            val start = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
            val end = start.plusDays(6)
            val fmt = DateTimeFormatter.ofPattern("d MMM", Locale("es"))
            return "${start.format(fmt)} – ${end.format(fmt)}"
        }

        val DAY_LABELS = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    }
}

class PlannerViewModelFactory(private val use: UseCases) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = PlannerViewModel(use) as T
}
