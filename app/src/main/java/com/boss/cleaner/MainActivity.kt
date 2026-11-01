package com.boss.cleaner

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        box.addView(TextView(this).apply {
            text = "boss - nettoyage automatique\n\nAccorde les autorisations dans l'ordre, puis affiche la bulle."
            textSize = 16f
        })
        box.addView(button("1. Afficher par-dessus les autres applis") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        box.addView(button("2. Accès aux statistiques d'utilisation") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        box.addView(button("3. Accès à tous les fichiers") {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
        })
        box.addView(button("Afficher la bulle") {
            startForegroundService(Intent(this, BubbleService::class.java))
        })
        box.addView(button("Arrêter la bulle et le nettoyage") {
            stopService(Intent(this, BubbleService::class.java))
            Scheduler.stop(this)
        })
        setContentView(box)
    }

    private fun button(label: String, onClick: () -> Unit) =
        Button(this).apply {
            text = label
            setOnClickListener { onClick() }
        }
}
