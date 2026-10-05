package com.startup.focuno.domain.model

/** MINDFUL is not a block: a short breathing pause before an app opens, with the choice to go on. */
enum class BlockKind { SCHEDULE, QUICK_BLOCK, DAILY_LIMIT, TIME_BUDGET, MINDFUL }

/** Everything the block screen needs to explain itself calmly. */
data class BlockOverlayModel(
    val packageName: String,
    val appName: String,
    val kind: BlockKind,
    /** APP blocks the whole app. SHORT_VIDEO blocks only Reels or Shorts inside it. */
    val scope: BlockScope = BlockScope.APP,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    /** Name shown for QUICK_BLOCK, e.g. "Focus session" or "Block now". */
    val quickLabel: String,
    val endsAtMs: Long,
    val strict: Boolean,
    /** When "I need this" works again, or null if it can be used now. */
    val bypassAvailableAtMs: Long?,
    /** For DAILY_LIMIT: the daily budget in minutes. */
    val limitMinutes: Int = 0,
    /** For DAILY_LIMIT reached by opens rather than time: the number of opens allowed. */
    val limitOpens: Int = 0,
    /** The person's exam and the days left to it, shown as a reminder; null days when none is set. */
    val examName: String = "",
    val examDaysLeft: Long? = null,
    /** False while any focus plan runs (even on a break), so "Focus instead" never replaces it. */
    val canFocusInstead: Boolean = true,
)
