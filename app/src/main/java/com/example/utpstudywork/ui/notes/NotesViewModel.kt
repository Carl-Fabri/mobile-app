package com.example.utpstudywork.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.utpstudywork.core.ClassificationResult
import com.example.utpstudywork.data.di.UseCases
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NoteStatus
import com.example.utpstudywork.domain.model.SessionType
import com.example.utpstudywork.domain.model.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class NotesUiState(
    val allNotes: List<Note> = emptyList(),
    val filteredNotes: List<Note> = emptyList(),
    val completedNotes: List<Note> = emptyList(),
    val selectedTab: SessionType? = null,
    val showCompleted: Boolean = false,
    val searchQuery: String = "",
    val showDialog: Boolean = false,
    val editingNote: Note? = null,
    val newTitle: String = "",
    val newDescription: String = "",
    val newCategory: SessionType = SessionType.WORK,
    val newColor: Int = 0xFFBBDEFB.toInt(),
    val newTaskText: String = "",
    val suggestion: ClassificationResult? = null,
    val suggestionDismissed: Boolean = false
)

class NotesViewModel(private val use: UseCases) : ViewModel() {

    private val _state = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = _state

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTab = MutableStateFlow<SessionType?>(null)
    private var suggestionJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                use.notesRepo.observeNotes(),
                _searchQuery,
                _selectedTab
            ) { notes, query, tab ->
                val active = notes
                    .filter { it.status != NoteStatus.COMPLETED }
                    .filter { tab == null || it.category == tab }
                    .filter {
                        query.isBlank() ||
                                it.title.contains(query, ignoreCase = true) ||
                                it.description.contains(query, ignoreCase = true)
                    }
                val completed = notes
                    .filter { it.status == NoteStatus.COMPLETED }
                    .filter {
                        query.isBlank() ||
                                it.title.contains(query, ignoreCase = true) ||
                                it.description.contains(query, ignoreCase = true)
                    }
                val updatedEditing = _state.value.editingNote?.id?.let { id -> notes.find { it.id == id } }
                _state.value.copy(
                    allNotes = notes,
                    filteredNotes = active,
                    completedNotes = completed,
                    searchQuery = query,
                    selectedTab = tab,
                    editingNote = updatedEditing ?: _state.value.editingNote
                )
            }.collect { _state.value = it }
        }
    }

    fun selectTab(type: SessionType?) {
        _selectedTab.value = type
        _state.value = _state.value.copy(showCompleted = false)
    }
    fun toggleShowCompleted() { _state.value = _state.value.copy(showCompleted = !_state.value.showCompleted) }
    fun updateSearch(query: String) { _searchQuery.value = query }

    fun openDialog(note: Note? = null) {
        _state.update {
            it.copy(
                showDialog = true,
                editingNote = note,
                newTitle = note?.title ?: "",
                newDescription = note?.description ?: "",
                newCategory = note?.category ?: SessionType.WORK,
                newColor = note?.color ?: 0xFFBBDEFB.toInt(),
                newTaskText = "",
                suggestion = null,
                suggestionDismissed = false
            )
        }
    }

    fun closeDialog() {
        suggestionJob?.cancel()
        _state.update {
            it.copy(
                showDialog = false,
                editingNote = null,
                newTitle = "",
                newDescription = "",
                newCategory = SessionType.WORK,
                newTaskText = "",
                suggestion = null,
                suggestionDismissed = false
            )
        }
    }

    fun updateTitle(title: String) {
        _state.update { it.copy(newTitle = title, suggestionDismissed = false) }
        scheduleClassification()
    }

    fun updateDescription(desc: String) {
        _state.update { it.copy(newDescription = desc, suggestionDismissed = false) }
        scheduleClassification()
    }

    fun updateCategory(cat: SessionType) {
        _state.update { it.copy(newCategory = cat, suggestion = null, suggestionDismissed = true) }
    }

    fun updateColor(color: Int) = _state.update { it.copy(newColor = color) }
    fun updateNewTaskText(text: String) = _state.update { it.copy(newTaskText = text) }

    // El usuario acepta la sugerencia del clasificador
    fun acceptSuggestion() {
        val suggestion = _state.value.suggestion ?: return
        _state.update { it.copy(newCategory = suggestion.suggested, suggestionDismissed = true) }
    }

    // El usuario descarta la sugerencia (elige manualmente)
    fun dismissSuggestion() {
        _state.update { it.copy(suggestionDismissed = true) }
    }

    fun saveNote() {
        val title = _state.value.newTitle.trim()
        if (title.isEmpty()) return

        val existing = _state.value.editingNote
        val finalCategory = _state.value.newCategory
        val note = existing?.copy(
            title = title,
            description = _state.value.newDescription.trim(),
            category = finalCategory,
            color = _state.value.newColor
        ) ?: Note(
            id = UUID.randomUUID().toString(),
            title = title,
            description = _state.value.newDescription.trim(),
            triggerAtMillis = System.currentTimeMillis(),
            category = finalCategory,
            color = _state.value.newColor
        )

        viewModelScope.launch {
            use.addNote(note)
            // Aprender de la elección final del usuario
            use.classifier.learn(note.title, note.description, note.category)
            closeDialog()
        }
    }

    fun deleteNote(id: String) { viewModelScope.launch { use.notesRepo.delete(id) } }

    fun addTaskToNote(noteId: String) {
        val text = _state.value.newTaskText.trim()
        if (text.isEmpty()) return
        val task = Task(id = UUID.randomUUID().toString(), noteId = noteId, text = text)
        viewModelScope.launch {
            use.notesRepo.upsertTask(task)
            _state.update { it.copy(newTaskText = "") }
        }
    }

    fun toggleTaskDone(task: Task) {
        viewModelScope.launch { use.notesRepo.setTaskDone(task.id, !task.isDone) }
    }

    fun deleteTask(id: String) { viewModelScope.launch { use.notesRepo.deleteTask(id) } }

    fun setStatus(id: String, status: NoteStatus) {
        viewModelScope.launch { use.notesRepo.setStatus(id, status) }
    }

    // Debounce de 400ms para no clasificar en cada keystroke
    private fun scheduleClassification() {
        suggestionJob?.cancel()
        suggestionJob = viewModelScope.launch {
            delay(400)
            val title = _state.value.newTitle
            val desc = _state.value.newDescription
            if (title.length < 3) {
                _state.update { it.copy(suggestion = null) }
                return@launch
            }
            val result = use.classifier.classify(title, desc)
            // Solo mostrar sugerencia con confianza mínima del 55%
            if (result.confidence >= 0.55f) {
                _state.update { it.copy(suggestion = result) }
            } else {
                _state.update { it.copy(suggestion = null) }
            }
        }
    }
}
