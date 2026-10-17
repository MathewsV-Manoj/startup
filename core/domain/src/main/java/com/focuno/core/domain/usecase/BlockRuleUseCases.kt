package com.focuno.core.domain.usecase

import com.focuno.core.domain.repository.BlockRuleRepository
import com.focuno.core.model.BlockRule
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveBlockRulesUseCase @Inject constructor(
    private val repository: BlockRuleRepository,
) {
    operator fun invoke(): Flow<List<BlockRule>> = repository.observeRules()
}

class UpsertBlockRuleUseCase @Inject constructor(
    private val repository: BlockRuleRepository,
) {
    suspend operator fun invoke(rule: BlockRule) = repository.upsert(rule)
}

class DeleteBlockRuleUseCase @Inject constructor(
    private val repository: BlockRuleRepository,
) {
    suspend operator fun invoke(packageName: String) = repository.delete(packageName)
}
