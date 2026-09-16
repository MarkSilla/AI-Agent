package com.marksilla.auraagent

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale

data class InstalledApp(
    val name: String,
    val packageName: String
)

fun getInstalledApps(context: Context): List<InstalledApp> {
    val packageManager = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val resolveInfos =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

    return resolveInfos
        .mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo?.packageName
                ?: return@mapNotNull null

            val appName = resolveInfo
                .loadLabel(packageManager)
                ?.toString()
                ?.trim()
                .orEmpty()

            if (appName.isBlank()) {
                null
            } else {
                InstalledApp(
                    name = appName,
                    packageName = packageName
                )
            }
        }
        .distinctBy { it.packageName }
        .sortedBy { it.name.lowercase(Locale.ROOT) }
}

fun findApp(
    apps: List<InstalledApp>,
    requestedName: String
): InstalledApp? {
    val requested = normalizeAppName(requestedName)

    if (requested.isBlank()) {
        return null
    }

    return apps.firstOrNull { normalizeAppName(it.name) == requested }
        ?: apps.firstOrNull { normalizeAppName(it.name).startsWith(requested) }
        ?: apps.firstOrNull { normalizeAppName(it.name).contains(requested) }
}

fun openApp(
    context: Context,
    app: InstalledApp
): Boolean {
    val launchIntent = context.packageManager
        .getLaunchIntentForPackage(app.packageName)
        ?: return false

    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    return runCatching {
        context.startActivity(launchIntent)
        true
    }.getOrDefault(false)
}

private fun normalizeAppName(name: String): String =
    name
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9\\s]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
