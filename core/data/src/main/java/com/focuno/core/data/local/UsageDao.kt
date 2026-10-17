package com.focuno.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM app_usage WHERE epochDay = :epochDay ORDER BY totalMillis DESC")
    fun observeForDay(epochDay: Long): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE epochDay = :epochDay ORDER BY totalMillis DESC")
    suspend fun getForDay(epochDay: Long): List<AppUsageEntity>

    @Query("SELECT * FROM app_usage WHERE epochDay BETWEEN :from AND :to")
    suspend fun getForRange(from: Long, to: Long): List<AppUsageEntity>

    @Upsert
    suspend fun upsertAll(rows: List<AppUsageEntity>)

    @Query("DELETE FROM app_usage WHERE epochDay < :beforeEpochDay")
    suspend fun deleteOlderThan(beforeEpochDay: Long)
}
