package com.focuno.core.data.repository

import com.focuno.core.model.AppCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assigns a default category to an app. User overrides always win. The starter
 * list is intentionally small; let users correct it in the analytics screen.
 */
@Singleton
class AppCategorizer @Inject constructor() {

    fun categorize(packageName: String, overrides: Map<String, AppCategory>): AppCategory =
        overrides[packageName] ?: DEFAULTS[packageName] ?: AppCategory.NEUTRAL

    private companion object {
        val DEFAULTS: Map<String, AppCategory> = buildMap {
            listOf(
                "com.instagram.android",
                "com.zhiliaoapp.musically",
                "com.google.android.youtube",
                "com.facebook.katana",
                "com.twitter.android",
                "com.snapchat.android",
                "com.reddit.frontpage",
                "com.netflix.mediaclient",
            ).forEach { put(it, AppCategory.DISTRACTING) }

            listOf(
                "com.notion.id",
                "com.google.android.apps.docs",
                "com.microsoft.office.word",
                "org.anki",
                "com.duolingo",
                "com.coursera.android",
                "com.google.android.apps.classroom",
                "com.zoom.us",
            ).forEach { put(it, AppCategory.PRODUCTIVE) }
        }
    }
}
