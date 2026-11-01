package com.boss.cleaner

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object Scheduler {
    const val INTERVAL_MS = 5L * 60 * 1000
    private const val PREFS = "boss"
    private const val KEY_ACTIVE = "active"
    private const val REQ_CODE = 4242

    fun isActive(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ACTIVE, false)

    fun start(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, true).apply()
        schedule(ctx)
    }

    fun stop(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ACTIVE, false).apply()
        ctx.getSystemService(AlarmManager::class.java).cancel(pendingIntent(ctx))
    }

    // Chaque tick reprogramme le suivant : Android peut retarder l'alarme en veille.
    fun schedule(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + INTERVAL_MS,
            pendingIntent(ctx)
        )
    }

    private fun pendingIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, REQ_CODE,
            Intent(ctx, TickReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
