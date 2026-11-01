package com.boss.cleaner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Scheduler.isActive(context)) return
        val app = context.applicationContext
        val pending = goAsync()
        Thread {
            try {
                CleanerEngine.runOnce(app)
            } finally {
                Scheduler.schedule(app)
                pending.finish()
            }
        }.start()
    }
}
