package com.startup.focuno.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.startup.focuno.R
import java.util.Locale

fun formatClock(minuteOfDay: Int): String = String.format(Locale.ROOT, "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)

/** "1h 20m", "45m" or "<1m". Rounds up so a countdown never shows 0 while time is left. */
@Composable
fun durationText(ms: Long, roundUp: Boolean = false): String {
    val totalMinutes = if (roundUp) (ms + 59_999) / 60_000 else ms / 60_000
    val hours = (totalMinutes / 60).toInt()
    val minutes = (totalMinutes % 60).toInt()
    return when {
        hours > 0 && minutes > 0 -> stringResource(R.string.duration_hours_minutes, hours, minutes)
        hours > 0 -> stringResource(R.string.duration_hours, hours)
        minutes > 0 -> stringResource(R.string.duration_minutes, minutes)
        else -> stringResource(R.string.duration_under_a_minute)
    }
}

/** "Reels" for Instagram, "Shorts" for YouTube. */
@Composable
fun shortVideoName(packageName: String): String =
    stringResource(if (packageName == com.startup.focuno.domain.model.ShortVideoApps.YOUTUBE) R.string.feed_shorts else R.string.feed_reels)
