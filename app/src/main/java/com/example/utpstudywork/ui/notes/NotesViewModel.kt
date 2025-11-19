package com.example.utpstudywork.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.utpstudywork.data.di.UseCases
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.SessionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class NotesUiState(
    val allNotes: List<Note> = emptyList(),
    val selectedTab: SessionType? = null,
    val showDialog: Boolean = false,
    val editingNote: Note? = null,
    val newTitle: String = "",
    val newDescription: String = "",
    val newCategory: SessionType = SessionType.WORK,
    val newColor: Int = 0xFFBBDEFB.toInt()
)

class NotesViewModel(private val use: UseCases) : ViewModel() {

    private val _state = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = _state

    init {
        viewModelScope.launch {
            use.notesRepo?.observeNotes()?.collect { notes ->
                _state.update { it.copy(allNotes = notes) }
            }
        }
    }

    fun selectTab(type: SessionType?) {
        _state.update { it.copy(selectedTab = type) }
    }

    fun openDialog(note: Note? = null) {
        _state.update {
            it.copy(
                showDialog = true,
                editingNote = note,
                newTitle = note?.title ?: "",
                newDescription = note?.description ?: "",
                newCategory = note?.category ?: SessionType.WORK,
                newColor = note?.color ?: 0xFFBBDEFB.toInt()
            )
        }
    }

    fun closeDialog() {
        _state.update {
            it.copy(
                showDialog = false,
                editingNote = null,
                newTitle = "",
                newDescription = "",
                newCategory = SessionType.WORK
            )
        }
    }

    fun updateTitle(title: String) {
        _state.update { it.copy(newTitle = title) }
    }

    fun updateDescription(description: String) {
        _state.update { it.copy(newDescription = description) }
    }

    fun updateCategory(category: SessionType) {
        _state.update { it.copy(newCategory = category) }
    }

    fun updateColor(color: Int) {
        _state.update { it.copy(newColor = color) }
    }

    fun saveNote() {
        val title = _state.value.newTitle.trim()
        val description = _state.value.newDescription.trim()

        if (title.isEmpty()) return

        val note = _state.value.editingNote?.copy(
            title = title,
            description = description,
            category = _state.value.newCategory,
            color = _state.value.newColor
        ) ?: Note(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            triggerAtMillis = System.currentTimeMillis(),
            category = _state.value.newCategory,
            color = _state.value.newColor
        )

        viewModelScope.launch {
            use.addNote(note)
            closeDialog()
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            use.notesRepo?.delete(id)
        }
    }

    fun getFilteredNotes(): List<Note> {
        return if (_state.value.selectedTab == null) {
            _state.value.allNotes
        } else {
            _state.value.allNotes.filter { it.category == _state.value.selectedTab }
        }
    }
}
