package com.focuno.core.domain.repository

import com.focuno.core.model.FocusScore
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface FocusScoreRepository {
    fun observeScore(date: LocalDate): Flow<FocusScore?>
    suspend fun getScore(date: LocalDate): FocusScore?
    fun observeRecentScores(days: Int): Flow<List<FocusScore>>
    suspend fun save(score: FocusScore)
}
