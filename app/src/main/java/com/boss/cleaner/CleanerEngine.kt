package com.boss.cleaner

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Règle de sécurité : rien n'est effacé définitivement. Les fichiers ciblés
// sont déplacés dans BossQuarantine/<date>/ et purgés après 7 jours.
object CleanerEngine {
    private const val TAG = "BossCleaner"

    // Jamais touchés : code, configuration, clés, données.
    private val PROTECTED_EXT = setOf(
        "kt", "java", "xml", "gradle", "kts", "json", "properties",
        "key", "pem", "keystore", "jks", "env", "db", "sqlite"
    )
    private val PROTECTED_NAME_PARTS = listOf("backup", "sauvegarde", "password", "key")
    private val JUNK_EXT = setOf("tmp", "temp", "class", "dex", "log", "crdownload", "part")
    private val SCAN_DIRS = listOf("Download", "DCIM", "Pictures", "Movies", "Music", "Documents")
    private val SKIP_DIRS = setOf("src", "build", "backup", "Backup", "Sauvegarde", "sauvegarde", "keys")

    private const val MIN_JUNK_AGE_MS = 60L * 60 * 1000          // 1 h
    private const val MIN_APK_AGE_MS = 3L * 24 * 3600 * 1000     // 3 jours
    private const val PURGE_AGE_MS = 7L * 24 * 3600 * 1000       // corbeille : 7 jours
    private const val MAX_DEPTH = 4

    private val root: File get() = Environment.getExternalStorageDirectory()
    private val quarantineRoot: File get() = File(root, "BossQuarantine")

    class Report(var cacheFreed: Long = 0, var moved: Int = 0, var purged: Int = 0, var closedApps: Int = 0)

    fun runOnce(ctx: Context): Report {
        val r = Report()
        r.cacheFreed = clearOwnCache(ctx)
        if (Environment.isExternalStorageManager()) {
            r.purged = purgeOldQuarantine()
            r.moved = scanAndQuarantine(ctx)
        } else {
            Log.w(TAG, "Accès à tous les fichiers non accordé : stockage ignoré")
        }
        r.closedApps = AppCloser.closeAllButRecent(ctx)
        Log.i(TAG, "cache=${r.cacheFreed}o déplacés=${r.moved} purgés=${r.purged} apps fermées=${r.closedApps}")
        return r
    }

    // Vide uniquement le cache de boss lui-même (Android interdit de vider celui des autres).
    private fun clearOwnCache(ctx: Context): Long {
        var freed = 0L
        listOfNotNull(ctx.cacheDir, ctx.externalCacheDir).forEach { dir ->
            dir.walkBottomUp().forEach { f ->
                if (f != dir) {
                    if (f.isFile) freed += f.length()
                    f.delete()
                }
            }
        }
        return freed
    }

    private fun scanAndQuarantine(ctx: Context): Int {
        val now = System.currentTimeMillis()
        val installed = ctx.packageManager.getInstalledPackages(0).map { it.packageName }.toSet()
        var moved = 0
        for (name in SCAN_DIRS) {
            val dir = File(root, name)
            if (!dir.isDirectory) continue
            walk(dir, 0) { f ->
                if (isProtected(f)) return@walk
                val ext = f.extension.lowercase(Locale.ROOT)
                val age = now - f.lastModified()
                val junk = ext in JUNK_EXT && age > MIN_JUNK_AGE_MS
                // APK dont l'application est déjà installée : l'appli reste intacte.
                val oldApk = ext == "apk" && age > MIN_APK_AGE_MS && isInstalledApk(ctx, f, installed)
                if ((junk || oldApk) && quarantine(f)) moved++
            }
        }
        return moved
    }

    private fun walk(dir: File, depth: Int, visit: (File) -> Unit) {
        if (depth > MAX_DEPTH) return
        dir.listFiles()?.forEach { f ->
            if (f.isDirectory) {
                if (!f.name.startsWith(".") && f.name !in SKIP_DIRS) walk(f, depth + 1, visit)
            } else if (f.isFile) {
                visit(f)
            }
        }
    }

    private fun isProtected(f: File): Boolean {
        if (f.extension.lowercase(Locale.ROOT) in PROTECTED_EXT) return true
        val lower = f.name.lowercase(Locale.ROOT)
        return PROTECTED_NAME_PARTS.any { lower.contains(it) }
    }

    private fun isInstalledApk(ctx: Context, f: File, installed: Set<String>): Boolean {
        val info = ctx.packageManager.getPackageArchiveInfo(f.absolutePath, 0) ?: return false
        return info.packageName in installed
    }

    private fun quarantine(f: File): Boolean {
        val day = SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(Date())
        val rel = f.absolutePath.removePrefix(root.absolutePath + "/")
        val target = File(File(quarantineRoot, day), rel)
        target.parentFile?.mkdirs()
        return f.renameTo(target)
    }

    private fun purgeOldQuarantine(): Int {
        val now = System.currentTimeMillis()
        var purged = 0
        quarantineRoot.listFiles()?.forEach { dayDir ->
            if (now - dayDir.lastModified() > PURGE_AGE_MS) {
                purged += dayDir.walkBottomUp().count { it.isFile }
                dayDir.deleteRecursively()
            }
        }
        return purged
    }
}
