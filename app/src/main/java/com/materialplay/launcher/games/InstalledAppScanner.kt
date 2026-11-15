package com.materialplay.launcher.games

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

data class ScannedApp(
    val packageName: String,
    val label: String,
    val isGame: Boolean,
    val isSystem: Boolean
)

data class ScanResult(
    val apps: List<ScannedApp>,
    val gameCount: Int
)

/**
 * Real package scanning: launcher activities plus the platform's game category.
 * No hard-coded package list, everything comes from PackageManager.
 */
class InstalledAppScanner(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    fun scan(): ScanResult {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val gameIntent = Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.GAME")

        val launcherResolve = queryActivities(launcherIntent)
        val declaredGames = queryActivities(gameIntent).mapNotNull {
            it.activityInfo?.applicationInfo?.packageName
        }.toSet()

        // ApplicationInfo.category catches games that do not advertise CATEGORY_GAME
        // on their launcher activity but do declare it on the application.
        val appCategories = runCatching {
            val apps = if (Build.VERSION.SDK_INT >= 33) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }
            apps.filter { it.category == ApplicationInfo.CATEGORY_GAME }
                .map { it.packageName }.toSet()
        }.getOrDefault(emptySet())

        val ownPackage = context.packageName
        val apps = launcherResolve.mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            val app = activity.applicationInfo ?: return@mapNotNull null
            val pkg = activity.packageName
            if (pkg == ownPackage) return@mapNotNull null
            val label = runCatching { activity.loadLabel(pm).toString() }
                .getOrDefault(pkg.substringAfterLast('.'))
            val isGame = pkg in declaredGames || pkg in appCategories ||
                app.category == ApplicationInfo.CATEGORY_GAME
            ScannedApp(
                packageName = pkg,
                label = label.takeIf { it.isNotBlank() } ?: pkg,
                isGame = isGame,
                isSystem = (app.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
            )
        }.distinctBy { it.packageName }

        return ScanResult(apps = apps, gameCount = apps.count { it.isGame })
    }

    fun isInstalled(packageName: String): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
        true
    }.getOrDefault(false)

    private fun queryActivities(intent: Intent) = runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
    }.getOrDefault(emptyList())
}
