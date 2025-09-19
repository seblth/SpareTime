package com.example.sparetimeapp.focus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sparetimeapp.R
import com.example.sparetimeapp.ui.MainActivity

object Notifications {
    private const val CH = "limits"

    private fun ensure(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CH) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CH, "Limits", NotificationManager.IMPORTANCE_HIGH)
                )
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun limitReached(ctx: Context, pkg: String, text: String = "Tageslimit erreicht") {
        ensure(ctx)
        val pi = PendingIntent.getActivity(
            ctx, 101, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.mipmap.ic_launcher) // quick & dirty
            .setContentTitle("Blockiert: ${pkg}")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(ctx).notify(pkg.hashCode(), notif)
    }
}