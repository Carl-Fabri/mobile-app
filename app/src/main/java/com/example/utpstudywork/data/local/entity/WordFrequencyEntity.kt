package com.example.utpstudywork.data.local.entity

import androidx.room.Entity

@Entity(tableName = "word_frequencies", primaryKeys = ["word", "category"])
data class WordFrequencyEntity(
    val word: String,
    val category: String,   // "WORK" o "STUDY"
    val count: Int = 1
)
