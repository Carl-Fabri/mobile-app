package com.example.utpstudywork.domain.model

data class Objective(
    val id: String,
    val weekId: String,
    val title: String,
    val orderIndex: Int = 0,
    val isDone: Boolean = false,
    val notes: List<Note> = emptyList()
)
