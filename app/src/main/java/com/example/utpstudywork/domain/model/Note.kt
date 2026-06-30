package com.example.utpstudywork.domain.model

data class Note(
    val id: String,
    val title: String,
    val description: String,
    val triggerAtMillis: Long,
    val category: SessionType = SessionType.WORK,
    val color: Int = 0xFFBBDEFB.toInt(),
    val tasks: List<Task> = emptyList(),
    val notification: NotificationConfig? = null,
    val isFocused: Boolean = false,
    val status: NoteStatus = NoteStatus.PENDING,
    val objectiveId: String? = null,
    val calendarWeekId: String? = null,
    val calendarDayOfWeek: Int? = null
)
