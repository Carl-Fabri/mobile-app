package com.example.utpstudywork.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.utpstudywork.data.local.entity.WeekEntity

@Dao
interface WeekDao {

    @Query("SELECT * FROM weeks WHERE id = :id LIMIT 1")
    suspend fun get(id: String): WeekEntity?

    @Upsert
    suspend fun upsert(entity: WeekEntity)
}
