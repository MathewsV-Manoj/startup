package com.focuno.core.data.repository

import com.focuno.core.data.local.FocusScoreDao
import com.focuno.core.data.mapper.toDomain
import com.focuno.core.data.mapper.toEntity
import com.focuno.core.domain.repository.FocusScoreRepository
import com.focuno.core.model.FocusScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFocusScoreRepository @Inject constructor(
    private val dao: FocusScoreDao,
) : FocusScoreRepository {

    override fun observeScore(date: LocalDate): Flow<FocusScore?> =
        dao.observe(date.toEpochDay()).map { it?.toDomain() }

    override suspend fun getScore(date: LocalDate): FocusScore? = dao.get(date.toEpochDay())?.toDomain()

    override fun observeRecentScores(days: Int): Flow<List<FocusScore>> {
        val from = LocalDate.now().minusDays((days - 1).toLong()).toEpochDay()
        return dao.observeFrom(from).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun save(score: FocusScore) = dao.upsert(score.toEntity())
}
