package com.focuno.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockRuleDao {
    @Query("SELECT * FROM block_rule ORDER BY packageName ASC")
    fun observeAll(): Flow<List<BlockRuleEntity>>

    @Upsert
    suspend fun upsert(entity: BlockRuleEntity)

    @Query("DELETE FROM block_rule WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
