package com.formacoach.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Lightweight, zero-polling lock-screen countdown notification for an active rest timer.
 * Uses Android's native countdown chronometer and degrades gracefully if notification permission is not granted.
 */
object RestTimerNotification {
    private const val CHANNEL_ID = "forma_rest_timer"
    private const val NOTIFICATION_ID = 4101

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, exerciseName: String, remainingSeconds: Long) {
        if (remainingSeconds <= 0L || !hasPermission(context)) {
            cancel(context)
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.rest_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val targetWallMillis = System.currentTimeMillis() + remainingSeconds * 1000L
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.rest_notification_title, exerciseName))
            .setContentText(context.getString(R.string.rest_notification_body))
            .setWhen(targetWallMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Dismiss itself at zero even if the app is killed mid-rest, so it never counts into negative time.
            .setTimeoutAfter(remainingSeconds * 1000L)
            .setContentIntent(openIntent)
            .build()

        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching { manager.cancel(NOTIFICATION_ID) }
    }
}
