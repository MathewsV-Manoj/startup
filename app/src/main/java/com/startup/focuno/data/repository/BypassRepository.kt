package com.startup.focuno.data.repository

import com.startup.focuno.data.room.BlockHitDao
import com.startup.focuno.data.room.BlockHitEntity
import com.startup.focuno.data.room.BypassEventDao
import com.startup.focuno.data.room.BypassEventEntity
import com.startup.focuno.domain.model.BypassOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Behavioural data that proves whether the product works: every block, every bypass stage. */
@Singleton
class BypassRepository @Inject constructor(
    private val bypassDao: BypassEventDao,
    private val hitDao: BlockHitDao,
) {
    suspend fun logEvent(packageName: String, outcome: BypassOutcome, reasonText: String?, nowMs: Long = System.currentTimeMillis()) {
        bypassDao.insert(BypassEventEntity(timestamp = nowMs, packageName = packageName, outcome = outcome, reasonText = reasonText))
    }

    suspend fun logBlockHit(packageName: String, nowMs: Long = System.currentTimeMillis()) {
        hitDao.insert(BlockHitEntity(timestamp = nowMs, packageName = packageName))
    }

    fun observeLastGrantedByPackage(): Flow<Map<String, Long>> =
        bypassDao.observeLastGranted().map { rows -> rows.associate { it.packageName to it.lastGrantedAt } }

    fun observeCount(outcome: BypassOutcome, sinceMs: Long): Flow<Int> = bypassDao.observeCount(outcome, sinceMs)

    fun observeBlockHitsSince(sinceMs: Long): Flow<Int> = hitDao.observeCountSince(sinceMs)

    suspend fun grantedBetween(fromMs: Long, toMs: Long): Int = bypassDao.grantedBetween(fromMs, toMs)
}
