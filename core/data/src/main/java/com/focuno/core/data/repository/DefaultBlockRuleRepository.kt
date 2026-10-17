package com.focuno.core.data.repository

import com.focuno.core.data.local.BlockRuleDao
import com.focuno.core.data.mapper.toDomain
import com.focuno.core.data.mapper.toEntity
import com.focuno.core.domain.repository.BlockRuleRepository
import com.focuno.core.model.BlockRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBlockRuleRepository @Inject constructor(
    private val dao: BlockRuleDao,
) : BlockRuleRepository {

    override fun observeRules(): Flow<List<BlockRule>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun upsert(rule: BlockRule) = dao.upsert(rule.toEntity())

    override suspend fun delete(packageName: String) = dao.delete(packageName)
}
