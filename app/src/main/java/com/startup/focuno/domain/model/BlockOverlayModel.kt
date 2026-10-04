package com.startup.focuno.domain.model

enum class BlockKind { SCHEDULE, QUICK_BLOCK }

/** Everything the block screen needs to explain itself calmly. */
data class BlockOverlayModel(
    val packageName: String,
    val appName: String,
    val kind: BlockKind,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    /** Name shown for QUICK_BLOCK, e.g. "Focus session" or "Block now". */
    val quickLabel: String,
    val endsAtMs: Long,
    val strict: Boolean,
    /** When "I need this" works again, or null if it can be used now. */
    val bypassAvailableAtMs: Long?,
)
