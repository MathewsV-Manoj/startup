package com.focuno.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusScoreDao {
    @Query("SELECT * FROM focus_score WHERE epochDay = :epochDay")
    fun observe(epochDay: Long): Flow<FocusScoreEntity?>

    @Query("SELECT * FROM focus_score WHERE epochDay = :epochDay")
    suspend fun get(epochDay: Long): FocusScoreEntity?

    @Query("SELECT * FROM focus_score WHERE epochDay >= :fromEpochDay ORDER BY epochDay ASC")
    fun observeFrom(fromEpochDay: Long): Flow<List<FocusScoreEntity>>

    @Upsert
    suspend fun upsert(entity: FocusScoreEntity)
}
