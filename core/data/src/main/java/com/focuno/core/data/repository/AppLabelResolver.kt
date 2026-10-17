package com.focuno.core.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.focuno.core.model.AppCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Resolves a human-readable app label from the package manager. */
@Singleton
class AppLabelResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun labelFor(packageName: String): String? = runCatching {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    }.getOrNull()
}
