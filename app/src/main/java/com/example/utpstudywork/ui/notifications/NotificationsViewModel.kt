package com.example.utpstudywork.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.utpstudywork.data.di.UseCases
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NotificationConfig
import com.example.utpstudywork.domain.model.NotificationType
import com.example.utpstudywork.domain.model.NoteStatus
import com.example.utpstudywork.domain.model.SessionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notes: List<Note> = emptyList(),
    // ID de la nota cuyo panel de configuración está expandido
    val expandedNoteId: String? = null
)

class NotificationsViewModel(private val use: UseCases) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state

    init {
        viewModelScope.launch {
            use.notesRepo.observeNotes().collect { notes ->
                _state.update { it.copy(notes = notes.filter { n -> n.status != NoteStatus.COMPLETED }) }
            }
        }
    }

    fun toggleExpand(noteId: String) {
        _state.update { s ->
            s.copy(expandedNoteId = if (s.expandedNoteId == noteId) null else noteId)
        }
    }

    fun setNoNotification(noteId: String) {
        updateNotification(noteId, null)
    }

    fun setDefaultNotification(noteId: String) {
        val note = _state.value.notes.find { it.id == noteId } ?: return
        updateNotification(noteId, NotificationConfig(type = NotificationType.DEFAULT))
    }

    fun setCustomNotification(noteId: String, message: String, filterCategory: SessionType?) {
        updateNotification(
            noteId,
            NotificationConfig(
                type = NotificationType.CUSTOM,
                customMessage = message,
                filterCategory = filterCategory
            )
        )
    }

    private fun updateNotification(noteId: String, config: NotificationConfig?) {
        val note = _state.value.notes.find { it.id == noteId } ?: return
        viewModelScope.launch {
            use.notesRepo.upsert(note.copy(notification = config))
        }
    }
}

class NotificationsViewModelFactory(private val use: UseCases) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = NotificationsViewModel(use) as T
}
