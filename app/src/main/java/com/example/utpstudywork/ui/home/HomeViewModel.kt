package com.example.utpstudywork.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.utpstudywork.data.di.UseCases
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NoteStatus
import com.example.utpstudywork.domain.model.SessionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class TimerUi(
    val type: SessionType = SessionType.WORK,
    val totalSec: Int = SessionType.WORK.defaultSeconds,
    val remainingSec: Int = SessionType.WORK.defaultSeconds,
    val running: Boolean = false
)

data class HomeUiState(
    val timer: TimerUi = TimerUi(),
    val workMinToday: Int = 0,
    val studyMinToday: Int = 0,
    val pendingNotes: List<Note> = emptyList(),    // solo PENDING — para el selector de foco
    val activeNotes: List<Note> = emptyList(),     // solo ACTIVE — para "En progreso"
    val focusedNotes: List<Note> = emptyList(),    // para el contador max 2
    val todayNotes: List<Note> = emptyList(),      // notas asignadas al día de hoy (cualquier estado)
    val showTodayNotes: Boolean = false            // expansión de la sección "notas de hoy"
)

class HomeViewModel(private val use: UseCases) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private var tickJob: Job? = null

    init {
        viewModelScope.launch { use.statsRepo.resetIfNewDay() }

        // Flujo principal: estadísticas + notas activas + notas en foco
        viewModelScope.launch {
            combine(
                use.statsRepo.observeToday(),
                use.notesRepo.observeActiveNotes(),
                use.notesRepo.observeFocusedNotes(),
                use.notesRepo.observePendingNotes()
            ) { stats, activeNotes, focused, pending ->
                _state.value.copy(
                    workMinToday = stats.workMinutes,
                    studyMinToday = stats.studyMinutes,
                    activeNotes = activeNotes,
                    focusedNotes = focused,
                    pendingNotes = pending
                )
            }.collect { _state.value = it }
        }

        // Flujo de notas de hoy del calendario (reactivo, tiempo real)
        val todayWeekId = weekIdForToday()
        val todayDow = LocalDate.now().dayOfWeek.value
        viewModelScope.launch {
            use.notesRepo.observeByWeekAndDay(todayWeekId, todayDow).collect { notes ->
                _state.update { it.copy(todayNotes = notes) }
            }
        }
    }

    // ── Navegación ──────────────────────────────────────────────────────────────

    fun toggleShowTodayNotes() { _state.update { it.copy(showTodayNotes = !it.showTodayNotes) } }

    // ── Timer ────────────────────────────────────────────────────────────────────

    fun switchTab(type: SessionType) {
        if (_state.value.timer.running) return
        val total = type.defaultSeconds
        tickJob?.cancel()
        _state.update {
            it.copy(timer = it.timer.copy(type = type, totalSec = total, remainingSec = total, running = false))
        }
    }

    fun startTimer() {
        if (_state.value.timer.running) return
        tickJob?.cancel()
        val currentSession = _state.value.timer.type
        use.notificationManager.triggerForSession(currentSession, _state.value.activeNotes)
        tickJob = viewModelScope.launch {
            val t = _state.value.timer
            use.startTimer(t.type, t.remainingSec).collect { remaining ->
                _state.update { it.copy(timer = it.timer.copy(remainingSec = remaining, running = true)) }
                use.notificationManager.updateTimerNotification(
                    remainingSec = remaining,
                    sessionType = _state.value.timer.type,
                    activeNotes = _state.value.activeNotes
                )
                if (remaining <= 0) { onFinishSession(); pauseTimer() }
            }
        }
    }

    fun pauseTimer() {
        use.pauseTimer(); tickJob?.cancel(); tickJob = null
        _state.update { it.copy(timer = it.timer.copy(running = false)) }
        use.notificationManager.cancelTimerNotification()
    }

    fun stopTimer() {
        use.stopTimer(); tickJob?.cancel(); tickJob = null
        val total = _state.value.timer.type.defaultSeconds
        _state.update { it.copy(timer = it.timer.copy(remainingSec = total, totalSec = total, running = false)) }
        use.notificationManager.cancelTimerNotification()
    }

    // ── Notas ────────────────────────────────────────────────────────────────────

    fun setNoteStatus(note: Note, status: NoteStatus) {
        viewModelScope.launch { use.notesRepo.setStatus(note.id, status) }
    }

    fun toggleFocus(note: Note) {
        val isCurrentlyFocused = note.isFocused
        if (!isCurrentlyFocused && _state.value.focusedNotes.size >= 2) return
        viewModelScope.launch {
            val newFocused = !isCurrentlyFocused
            use.notesRepo.setFocused(note.id, newFocused)
            if (newFocused) {
                // Activar la nota → pasa a "En progreso"
                use.notesRepo.setStatus(note.id, NoteStatus.ACTIVE)
            } else {
                // Des-activar → vuelve a PENDING
                use.notesRepo.setStatus(note.id, NoteStatus.PENDING)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        use.notificationManager.cancelTimerNotification()
    }

    private fun onFinishSession() = viewModelScope.launch {
        val type = _state.value.timer.type
        val minutes = type.defaultMinutes
        use.statsRepo.addMinutes(
            workDelta = if (type == SessionType.WORK) minutes else 0,
            studyDelta = if (type == SessionType.STUDY) minutes else 0
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun weekIdForToday(): String {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val millis = monday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
    }
}
