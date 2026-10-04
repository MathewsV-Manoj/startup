package com.startup.focuno.domain.model

/** What a schedule blocks: the whole app, or only its endless short-video feed (Reels, Shorts). */
enum class BlockScope { APP, SHORT_VIDEO }

/** Apps whose short-video feed can be blocked on its own. */
object ShortVideoApps {
    const val INSTAGRAM = "com.instagram.android"
    const val YOUTUBE = "com.google.android.youtube"

    val supported: Set<String> = setOf(INSTAGRAM, YOUTUBE)

    fun supports(packageName: String?): Boolean = packageName != null && packageName in supported
}

/** daysOfWeekMask: bit 0 = Monday ... bit 6 = Sunday. The day is the one the window STARTS on. */
data class BlockSchedule(
    val id: Long = 0,
    val packageName: String,
    val scope: BlockScope = BlockScope.APP,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeekMask: Int,
    val enabled: Boolean = true,
    val strictMode: Boolean = false,
) {
    fun isDayEnabled(dayIndexFromMonday: Int): Boolean = daysOfWeekMask and (1 shl dayIndexFromMonday) != 0

    companion object {
        const val ALL_DAYS = 0b1111111
    }
}

/** A daily time budget for one app. Once today's use reaches it, the app is paused until midnight. */
data class AppLimit(
    val packageName: String,
    val dailyMinutes: Int,
    val strict: Boolean = false,
    val enabled: Boolean = true,
) {
    val dailyMs: Long get() = dailyMinutes * 60_000L
}

data class ActiveBlockWindow(
    val endsAtMs: Long,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val strict: Boolean,
)
