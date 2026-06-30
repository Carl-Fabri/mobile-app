package com.example.utpstudywork.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weeks")
data class WeekEntity(
    @PrimaryKey val id: String,      // fecha ISO del lunes, ej. "2026-06-22"
    val startMillis: Long
)
