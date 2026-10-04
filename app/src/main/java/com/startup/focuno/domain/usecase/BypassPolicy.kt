package com.startup.focuno.domain.usecase

/** The cost of a bypass: 5 minutes of access, then 30 minutes before it can be asked for again. */
object BypassPolicy {
    const val FRICTION_COUNTDOWN_SECONDS = 15
    const val MIN_REASON_LENGTH = 15
    const val UNLOCK_MS = 5 * 60_000L
    const val COOLDOWN_MS = 30 * 60_000L

    fun isUnlocked(lastGrantedAtMs: Long?, nowMs: Long): Boolean =
        lastGrantedAtMs != null && nowMs >= lastGrantedAtMs && nowMs < lastGrantedAtMs + UNLOCK_MS

    /** When a new bypass may be requested again, or null if one has never been granted. */
    fun availableAgainAtMs(lastGrantedAtMs: Long?): Long? = lastGrantedAtMs?.plus(UNLOCK_MS + COOLDOWN_MS)

    fun canRequest(lastGrantedAtMs: Long?, nowMs: Long): Boolean {
        val availableAt = availableAgainAtMs(lastGrantedAtMs) ?: return true
        return nowMs >= availableAt
    }

    fun isReasonValid(reason: String): Boolean = reason.trim().length >= MIN_REASON_LENGTH
}
