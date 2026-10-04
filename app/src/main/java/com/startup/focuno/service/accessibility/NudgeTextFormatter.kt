package com.startup.focuno.service.accessibility

import android.content.Context
import com.startup.focuno.R
import com.startup.focuno.domain.usecase.Nudge
import com.startup.focuno.domain.usecase.NudgeKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Turns a nudge into a warm, specific sentence. All wording lives in strings.xml. */
@Singleton
class NudgeTextFormatter @Inject constructor(@ApplicationContext private val context: Context) {

    fun format(nudge: Nudge): String {
        val app = nudge.appName ?: context.getString(R.string.nudge_this_app)
        return when (nudge.kind) {
            NudgeKind.CONTINUOUS_USE -> context.resources.getQuantityString(R.plurals.nudge_continuous, nudge.minutes, app, nudge.minutes)
            NudgeKind.REPEATED_OPENS -> context.getString(R.string.nudge_repeated_opens, app, nudge.count)
            NudgeKind.GOAL_PROGRESS -> context.getString(R.string.nudge_goal_progress, nudge.percent)
            NudgeKind.AFTER_FOCUS_SESSION -> context.getString(R.string.nudge_after_focus, app)
        }
    }
}
