package com.example.utpstudywork.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.utpstudywork.data.local.entity.WordFrequencyEntity

@Dao
interface WordFrequencyDao {

    @Query("SELECT * FROM word_frequencies")
    suspend fun getAll(): List<WordFrequencyEntity>

    @Query("SELECT * FROM word_frequencies WHERE word = :word AND category = :category LIMIT 1")
    suspend fun get(word: String, category: String): WordFrequencyEntity?

    @Upsert
    suspend fun upsert(entity: WordFrequencyEntity)

    @Query("DELETE FROM word_frequencies")
    suspend fun clearAll()
}
