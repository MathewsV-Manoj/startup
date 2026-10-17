package com.focuno.core.domain.repository

import com.focuno.core.model.BlockRule
import kotlinx.coroutines.flow.Flow

interface BlockRuleRepository {
    fun observeRules(): Flow<List<BlockRule>>
    suspend fun upsert(rule: BlockRule)
    suspend fun delete(packageName: String)
}
