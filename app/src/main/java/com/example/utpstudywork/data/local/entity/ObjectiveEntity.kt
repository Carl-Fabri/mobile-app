package com.example.utpstudywork.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "objectives",
    foreignKeys = [
        ForeignKey(
            entity = WeekEntity::class,
            parentColumns = ["id"],
            childColumns = ["weekId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("weekId")]
)
data class ObjectiveEntity(
    @PrimaryKey val id: String,
    val weekId: String,
    val title: String,
    val orderIndex: Int = 0,
    val isDone: Boolean = false
)
