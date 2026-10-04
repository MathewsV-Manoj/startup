package com.startup.focuno.data.repository

import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.room.BypassEventDao
import com.startup.focuno.data.room.FocusSessionDao
import com.startup.focuno.data.room.FocusSessionEntity
import com.startup.focuno.data.room.SubjectTotal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** A focus session is a timed stretch where every distracting app is blocked (with the usual friction). */
@Singleton
class FocusSessionRepository @Inject constructor(
    private val dao: FocusSessionDao,
    private val bypassDao: BypassEventDao,
    private val settings: SettingsStore,
) {
    suspend fun start(plannedMs: Long, label: String, strict: Boolean, subject: String = "", nowMs: Long = System.currentTimeMillis()) {
        finalizeDue(nowMs)
        dao.insert(
            FocusSessionEntity(startTs = nowMs, endTs = nowMs + plannedMs, plannedMs = plannedMs, completed = false, interruptions = 0, subject = subject),
        )
        settings.setQuickBlock(nowMs + plannedMs, label, strict)
    }

    suspend fun active(nowMs: Long = System.currentTimeMillis()): FocusSessionEntity? {
        finalizeDue(nowMs)
        return dao.active(nowMs)
    }

    /** Ends the running session now. Returns false, and changes nothing, while a strict session is running. */
    suspend fun stopEarly(nowMs: Long = System.currentTimeMillis()): Boolean {
        val session = dao.active(nowMs) ?: return true
        val settingsNow = settings.settings.first()
        if (settingsNow.quickBlockStrict && settingsNow.quickBlockUntilMs > nowMs) return false
        val interruptions = bypassDao.grantedBetween(session.startTs, nowMs)
        dao.update(session.copy(endTs = nowMs, interruptions = interruptions, completed = false))
        settings.clearQuickBlock()
        return true
    }

    /** Marks sessions that ran their full length as completed and records how often they were interrupted. */
    suspend fun finalizeDue(nowMs: Long = System.currentTimeMillis()) {
        dao.finalizeDue(nowMs)
    }

    suspend fun lastCompletedEndMs(): Long? = dao.lastCompletedEnd()

    suspend fun completedBetween(fromMs: Long, toMs: Long): Int = dao.completedBetween(fromMs, toMs)

    fun observeCompletedCount(sinceMs: Long): Flow<Int> = dao.observeCompletedCount(sinceMs)

    fun observeSubjectTotals(sinceMs: Long, nowMs: Long): Flow<List<SubjectTotal>> = dao.observeSubjectTotals(sinceMs, nowMs)
}
