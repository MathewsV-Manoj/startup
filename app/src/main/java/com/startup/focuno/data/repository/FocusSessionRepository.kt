package com.startup.focuno.data.repository

import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.room.BypassEventDao
import com.startup.focuno.data.room.FocusSessionDao
import com.startup.focuno.data.room.FocusSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** A focus session is a timed stretch where every distracting app is blocked (with the usual friction). */
@Singleton
class FocusSessionRepository @Inject constructor(
    private val dao: FocusSessionDao,
    private val bypassDao: BypassEventDao,
    private val settings: SettingsStore,
) {
    suspend fun start(plannedMs: Long, label: String, nowMs: Long = System.currentTimeMillis()) {
        finalizeDue(nowMs)
        dao.insert(FocusSessionEntity(startTs = nowMs, endTs = nowMs + plannedMs, plannedMs = plannedMs, completed = false, interruptions = 0))
        settings.setQuickBlock(nowMs + plannedMs, label)
    }

    suspend fun active(nowMs: Long = System.currentTimeMillis()): FocusSessionEntity? {
        finalizeDue(nowMs)
        return dao.active(nowMs)
    }

    suspend fun stopEarly(nowMs: Long = System.currentTimeMillis()) {
        val session = dao.active(nowMs) ?: return
        val interruptions = bypassDao.grantedBetween(session.startTs, nowMs)
        dao.update(session.copy(endTs = nowMs, interruptions = interruptions, completed = false))
        settings.clearQuickBlock()
    }

    /** Marks sessions that ran their full length as completed and records how often they were interrupted. */
    suspend fun finalizeDue(nowMs: Long = System.currentTimeMillis()) {
        dao.finalizeDue(nowMs)
    }

    suspend fun lastCompletedEndMs(): Long? = dao.lastCompletedEnd()

    fun observeCompletedCount(sinceMs: Long): Flow<Int> = dao.observeCompletedCount(sinceMs)
}
