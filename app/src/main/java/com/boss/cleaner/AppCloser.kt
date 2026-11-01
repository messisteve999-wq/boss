package com.boss.cleaner

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent

object AppCloser {

    // Ces paquets ne sont jamais fermés.
    private val ALWAYS_KEEP = setOf("android", "com.android.systemui")

    fun recentPackages(ctx: Context, count: Int = 3): List<String> {
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, now - 2L * 24 * 3600 * 1000, now
        ) ?: return emptyList()
        val home = homePackage(ctx)
        return stats
            .filter { it.lastTimeUsed > 0 && it.packageName != ctx.packageName && it.packageName != home }
            .sortedByDescending { it.lastTimeUsed }
            .map { it.packageName }
            .distinct()
            .take(count)
    }

    // Ferme les applis en arrière-plan sauf les 3 dernières utilisées.
    // Android n'autorise pas de forcer l'arrêt d'une appli visible sans root/ADB.
    fun closeAllButRecent(ctx: Context): Int {
        val am = ctx.getSystemService(ActivityManager::class.java) ?: return 0
        val keep = mutableSetOf(ctx.packageName)
        keep.addAll(ALWAYS_KEEP)
        keep.addAll(recentPackages(ctx))
        homePackage(ctx)?.let { keep.add(it) }

        var closed = 0
        am.runningAppProcesses?.forEach { p ->
            p.pkgList.forEach { pkg ->
                if (pkg !in keep) {
                    am.killBackgroundProcesses(pkg)
                    closed++
                }
            }
        }
        return closed
    }

    private fun homePackage(ctx: Context): String? =
        ctx.packageManager
            .resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            ?.activityInfo?.packageName
}
