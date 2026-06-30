package com.example.utpstudywork.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = ObjectiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["objectiveId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("objectiveId"), Index("calendarWeekId")]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val triggerAtMillis: Long,
    val category: String,
    val color: Int,
    val isFocused: Boolean = false,
    // null = sin notificación, "DEFAULT" o "CUSTOM"
    val notifType: String? = null,
    val notifCustomMessage: String = "",
    // null = cualquier sesión, "WORK" o "STUDY"
    val notifFilterCategory: String? = null,
    // "PENDING", "ACTIVE" o "COMPLETED"
    val status: String = "PENDING",
    // Planner calendar fields
    val objectiveId: String? = null,
    val calendarWeekId: String? = null,
    val calendarDayOfWeek: Int? = null
)
