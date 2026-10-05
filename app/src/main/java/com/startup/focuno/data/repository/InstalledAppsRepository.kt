package com.startup.focuno.data.repository

import android.content.Context
import android.content.pm.PackageManager
import com.startup.focuno.data.local.launcherPackages
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledApp(val packageName: String, val label: String)

@Singleton
class InstalledAppsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val labelCache = ConcurrentHashMap<String, String>()

    /** Apps with a launcher icon, A to Z, without Focuno itself. */
    suspend fun installedApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.launcherPackages()
            .filter { it != context.packageName }
            .map { InstalledApp(it, labelBlocking(it)) }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun label(packageName: String): String = withContext(Dispatchers.IO) { labelBlocking(packageName) }

    /** Cheap after the first call. Safe to use from ViewModels that already run off the main thread. */
    fun labelBlocking(packageName: String): String = labelCache.getOrPut(packageName) {
        try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            fallbackLabel(packageName)
        }
    }

    private fun fallbackLabel(packageName: String): String = KNOWN_NAMES[packageName]
        ?: packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }

    private companion object {
        val KNOWN_NAMES = mapOf(
            "com.android.incallui" to "Phone call",
            "com.google.android.gms" to "Google Play services",
            "com.google.android.permissioncontroller" to "Permissions",
            "com.android.permissioncontroller" to "Permissions",
            "com.google.android.packageinstaller" to "Package installer",
            "com.android.vending" to "Play Store",
        )
    }
}
