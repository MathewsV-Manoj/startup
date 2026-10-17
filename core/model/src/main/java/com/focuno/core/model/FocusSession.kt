package com.focuno.core.model

import java.time.Instant

/**
 * A contiguous block of time the user spent focused on a single app (or a small
 * set of productive apps). Built by the data layer from raw usage events.
 *
 * @param startedAt session start
 * @param endedAt session end, null while the session is still open
 * @param dominantPackage the app that dominated the session
 * @param interrupted whether a distraction event was detected inside the session
 */
data class FocusSession(
    val startedAt: Instant,
    val endedAt: Instant?,
    val dominantPackage: String,
    val interrupted: Boolean,
) {
    val durationOrNull: Duration?
        get() = endedAt?.let { Duration.between(startedAt, it) }

    val isOpen: Boolean get() = endedAt == null
}
