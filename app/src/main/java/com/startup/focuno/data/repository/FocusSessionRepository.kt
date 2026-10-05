package com.startup.focuno.data.repository

import com.startup.focuno.data.local.SettingsStore
import com.startup.focuno.data.room.BypassEventDao
import com.startup.focuno.data.room.FocusSessionDao
import com.startup.focuno.data.room.FocusSessionEntity
import com.startup.focuno.data.room.SubjectTotal
import com.startup.focuno.domain.model.FocusMode
import com.startup.focuno.domain.model.FocusPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** A focus session is a timed stretch where every time-eater app is paused (with the usual friction unless strict). */
@Singleton
class FocusSessionRepository @Inject constructor(
    private val dao: FocusSessionDao,
    private val bypassDao: BypassEventDao,
    private val settings: SettingsStore,
) {
    /**
     * Starts [plan]. A Pomodoro gets one row per focus round up front, so each finished round counts as a
     * finished session; breaks have no row.
     */
    suspend fun start(plan: FocusPlan, label: String, strict: Boolean, subject: String = "") {
        finalizeDue(plan.startMs)
        // A new plan replaces any earlier one (for example a Pomodoro that is on a break): its running round
        // ends now unfinished, and rounds still to come are removed so they can never count as finished.
        dao.active(plan.startMs)?.let { dao.update(it.copy(endTs = plan.startMs, completed = false)) }
        dao.deleteUpcoming(plan.startMs)
        if (plan.mode == FocusMode.POMODORO) {
            repeat(plan.rounds) { index ->
                val roundStart = plan.startMs + index * (plan.roundMs + plan.breakMs)
                dao.insert(session(roundStart, roundStart + plan.roundMs, subject))
            }
        } else {
            dao.insert(session(plan.startMs, plan.endMs, subject))
        }
        settings.setFocusPlan(plan, label, strict)
    }

    private fun session(startMs: Long, endMs: Long, subject: String) =
        FocusSessionEntity(startTs = startMs, endTs = endMs, plannedMs = endMs - startMs, completed = false, interruptions = 0, subject = subject)

    suspend fun active(nowMs: Long = System.currentTimeMillis()): FocusSessionEntity? {
        finalizeDue(nowMs)
        return dao.active(nowMs)
    }

    /**
     * Ends the running plan now, including any Pomodoro rounds still to come. Returns false, and changes
     * nothing, while a strict plan is running. A stopwatch that ran long enough counts as finished.
     */
    suspend fun stopEarly(nowMs: Long = System.currentTimeMillis()): Boolean {
        val settingsNow = settings.settings.first()
        val plan = settingsNow.focusPlan
        if (settingsNow.quickBlockStrict && plan?.isRunning(nowMs) == true) return false
        dao.active(nowMs)?.let { session ->
            val interruptions = bypassDao.grantedBetween(session.startTs, nowMs)
            val finished = plan?.mode == FocusMode.STOPWATCH && nowMs - session.startTs >= STOPWATCH_COUNTS_AFTER_MS
            dao.update(session.copy(endTs = nowMs, interruptions = interruptions, completed = finished))
        }
        dao.deleteUpcoming(nowMs)
        settings.clearQuickBlock()
        return true
    }

    /** Marks sessions that ran their full length as completed and records how often they were interrupted. */
    suspend fun finalizeDue(nowMs: Long = System.currentTimeMillis()) {
        dao.finalizeDue(nowMs)
    }

    suspend fun lastCompletedEndMs(): Long? = dao.lastCompletedEnd()

    /** Start and end of every bit of focus since [sinceMs]; a running session ends at [nowMs]. */
    suspend fun focusSpans(sinceMs: Long, nowMs: Long): List<Pair<Long, Long>> =
        dao.startedBetween(sinceMs, nowMs).map { it.startTs to minOf(it.endTs, nowMs) }

    suspend fun completedBetween(fromMs: Long, toMs: Long): Int = dao.completedBetween(fromMs, toMs)

    fun observeCompletedCount(sinceMs: Long): Flow<Int> = dao.observeCompletedCount(sinceMs)

    fun observeSubjectTotals(sinceMs: Long, nowMs: Long): Flow<List<SubjectTotal>> = dao.observeSubjectTotals(sinceMs, nowMs)

    fun observeRecent(nowMs: Long, limit: Int): Flow<List<FocusSessionEntity>> = dao.observeRecent(nowMs, limit)

    companion object {
        /** A stopwatch has no planned length, so it counts as a finished session once it ran this long. */
        const val STOPWATCH_COUNTS_AFTER_MS = 10 * 60_000L

        /** Safety cap so a forgotten stopwatch cannot block apps for ever. */
        const val STOPWATCH_MAX_MS = 4 * 60 * 60_000L
    }
}
