package com.example.utpstudywork.domain.model

// id = fecha ISO del lunes de esa semana, ej. "2026-06-22"
data class Week(
    val id: String,
    val startMillis: Long
)
