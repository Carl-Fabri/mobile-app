package com.example.utpstudywork.domain.model

data class Task(
    val id: String,
    val noteId: String,
    val text: String,
    val isDone: Boolean = false
)
