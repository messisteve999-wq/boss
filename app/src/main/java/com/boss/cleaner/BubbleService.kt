package com.boss.cleaner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs

class BubbleService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Autorisation d'affichage manquante", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }
        val channel = NotificationChannel(CHANNEL, "boss", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notif = Notification.Builder(this, CHANNEL)
            .setContentTitle("boss actif")
            .setContentText("Bulle flottante en service")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .build()
        startForeground(NOTIF_ID, notif)

        wm = getSystemService(WindowManager::class.java)
        addBubble()
    }

    private fun addBubble() {
        val size = (56 * resources.displayMetrics.density).toInt()
        val tv = TextView(this).apply {
            text = "B"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorFor())
            }
        }
        params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        var downX = 0f; var downY = 0f
        var startX = 0; var startY = 0
        var moved = false

        tv.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = params.x; startY = params.y
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    params.x = startX + dx.toInt()
                    params.y = startY + dy.toInt()
                    wm.updateViewLayout(tv, params)
                }
                MotionEvent.ACTION_UP -> if (!moved) toggle(tv)
            }
            true
        }
        bubble = tv
        wm.addView(tv, params)
    }

    private fun toggle(tv: TextView) {
        if (Scheduler.isActive(this)) {
            Scheduler.stop(this)
            Toast.makeText(this, "Nettoyage désactivé", Toast.LENGTH_SHORT).show()
        } else {
            Scheduler.start(this)
            Thread { CleanerEngine.runOnce(applicationContext) }.start()
            Toast.makeText(this, "Nettoyage activé (toutes les 5 min)", Toast.LENGTH_SHORT).show()
        }
        (tv.background as GradientDrawable).setColor(colorFor())
    }

    private fun colorFor(): Int =
        if (Scheduler.isActive(this)) Color.parseColor("#2E7D32") else Color.parseColor("#616161")

    override fun onDestroy() {
        bubble?.let { wm.removeView(it) }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "boss"
        const val NOTIF_ID = 1
    }
}
