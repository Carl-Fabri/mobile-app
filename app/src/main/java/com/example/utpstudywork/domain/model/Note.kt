package com.example.utpstudywork.domain.model

import com.example.utpstudywork.domain.model.SessionType

data class Note(
    val id: String,
    val title: String,
    val description: String,
    val triggerAtMillis: Long,
    val category: SessionType = SessionType.WORK,
    val color: Int = 0xFFBBDEFB.toInt()
)
