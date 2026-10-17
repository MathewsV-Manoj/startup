package com.focuno.core.domain.usecase

import com.focuno.core.common.FocunoError
import com.focuno.core.common.FocunoResult
import com.focuno.core.domain.repository.FocusScoreRepository
import com.focuno.core.domain.repository.UsageRepository
import com.focuno.core.domain.repository.UserFocusProfileRepository
import com.focuno.core.domain.score.FocusScoreEngine
import com.focuno.core.model.FocusScore
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Recomputes and persists the day's score from current usage data.
 * Session materialization lives in the data layer; here we pass an empty list
 * until Milestone 1 wires real sessions, which falls back to productive-usage ratio.
 */
class ComputeFocusScoreUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val scoreRepository: FocusScoreRepository,
    private val profileRepository: UserFocusProfileRepository,
) {
    suspend operator fun invoke(date: LocalDate): FocunoResult<FocusScore> = try {
        val usages = usageRepository.usageFor(date)
        val profile = profileRepository.current()
        val consistency = consistencyFor(date, profile.consistencyWindowDays)
        val score = FocusScoreEngine.compute(
            date = date,
            usages = usages,
            sessions = emptyList(),
            consistency = consistency,
            profile = profile,
        )
        scoreRepository.save(score)
        FocunoResult.Success(score)
    } catch (t: Throwable) {
        FocunoResult.Failure(FocunoError.Platform(t.message ?: "score computation failed", t))
    }

    private suspend fun consistencyFor(date: LocalDate, window: Int): Float {
        // TODO(Milestone 2): derive from stored recent scores once history exists.
        return DEFAULT_CONSISTENCY
    }

    private companion object {
        const val DEFAULT_CONSISTENCY = 0.8f
    }
}

class ObserveFocusScoreUseCase @Inject constructor(
    private val repository: FocusScoreRepository,
) {
    operator fun invoke(date: LocalDate): Flow<FocusScore?> = repository.observeScore(date)
}

class ObserveRecentScoresUseCase @Inject constructor(
    private val repository: FocusScoreRepository,
) {
    operator fun invoke(days: Int): Flow<List<FocusScore>> = repository.observeRecentScores(days)
}
