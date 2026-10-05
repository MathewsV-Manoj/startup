package com.startup.focuno.data.local

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

// Android 13 replaced the Int-flags overloads with ResolveInfoFlags. Below API 33 there is no replacement,
// so the legacy call is isolated here, in one place, behind a version check.

fun PackageManager.queryActivitiesCompat(intent: Intent, flags: Int = 0): List<ResolveInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
    } else {
        legacyQueryActivities(intent, flags)
    }

@Suppress("DEPRECATION")
private fun PackageManager.legacyQueryActivities(intent: Intent, flags: Int): List<ResolveInfo> =
    queryIntentActivities(intent, flags)

/** Packages that have a launcher icon, i.e. apps a person actually opens. */
fun PackageManager.launcherPackages(): Set<String> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return queryActivitiesCompat(intent).mapTo(HashSet()) { it.activityInfo.packageName }
}

/** Every installed home screen app (the default one and any others). */
fun PackageManager.homePackages(): Set<String> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return queryActivitiesCompat(intent, PackageManager.MATCH_DEFAULT_ONLY).mapTo(HashSet()) { it.activityInfo.packageName }
}
