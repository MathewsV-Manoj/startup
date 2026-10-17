package com.focuno.feature.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focuno.core.domain.usecase.DeleteBlockRuleUseCase
import com.focuno.core.domain.usecase.ObserveBlockRulesUseCase
import com.focuno.core.domain.usecase.UpsertBlockRuleUseCase
import com.focuno.core.model.BlockRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockingViewModel @Inject constructor(
    observeBlockRules: ObserveBlockRulesUseCase,
    private val upsertRule: UpsertBlockRuleUseCase,
    private val deleteRule: DeleteBlockRuleUseCase,
) : ViewModel() {

    val rules: StateFlow<List<BlockRule>> = observeBlockRules()
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun setEnabled(rule: BlockRule, enabled: Boolean) {
        viewModelScope.launch { upsertRule(rule.copy(enabled = enabled)) }
    }

    fun delete(rule: BlockRule) {
        viewModelScope.launch { deleteRule(rule.packageName) }
    }
}
