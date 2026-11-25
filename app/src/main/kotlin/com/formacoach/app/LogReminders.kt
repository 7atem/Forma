package com.formacoach.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.formacoach.coach.ReminderTime
import java.time.LocalDateTime
import java.time.ZoneId

/** Optional daily logging nudges. Off by default; text never includes logged data. */
internal enum class ReminderKind(val requestCode: Int, val defaultMinute: Int, val label: Int, val body: Int) {
    MEALS(5101, 13 * 60, R.string.reminder_meals, R.string.reminder_meals_body),
    WATER(5102, 11 * 60, R.string.reminder_water, R.string.reminder_water_body),
    WORKOUT(5103, 18 * 60, R.string.reminder_workout, R.string.reminder_workout_body),
}

internal data class ReminderSetting(val enabled: Boolean, val minuteOfDay: Int)

internal object LogReminders {
    private const val PREFS = "forma_ui_prefs"
    private const val CHANNEL_ID = "forma_log_reminders"
    private const val EXTRA_KIND = "forma_reminder_kind"

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun read(context: Context, kind: ReminderKind): ReminderSetting {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val minute = prefs.getInt("reminder_${kind.name}_minute", kind.defaultMinute).coerceIn(0, ReminderTime.MINUTES_PER_DAY - 1)
        return ReminderSetting(prefs.getBoolean("reminder_${kind.name}_on", false), minute)
    }

    fun save(context: Context, kind: ReminderKind, setting: ReminderSetting) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("reminder_${kind.name}_on", setting.enabled)
            .putInt("reminder_${kind.name}_minute", setting.minuteOfDay)
            .apply()
        schedule(context, kind, setting)
    }

    /** Re-arms every enabled reminder; safe to call repeatedly (app start, reboot, time-zone change). */
    fun scheduleAll(context: Context) = ReminderKind.entries.forEach { schedule(context, it, read(context, it)) }

    private fun pendingIntent(context: Context, kind: ReminderKind): PendingIntent = PendingIntent.getBroadcast(
        context,
        kind.requestCode,
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_KIND, kind.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun schedule(context: Context, kind: ReminderKind, setting: ReminderSetting) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = pendingIntent(context, kind)
        alarms.cancel(intent)
        if (!setting.enabled) return
        val zone = ZoneId.systemDefault()
        val next = ReminderTime.next(LocalDateTime.now(zone), setting.minuteOfDay).atZone(zone).toInstant().toEpochMilli()
        // Inexact on purpose: no exact-alarm permission, and the system may batch it to save battery.
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, next, AlarmManager.INTERVAL_DAY, intent)
    }

    fun show(context: Context, kind: ReminderKind) {
        if (!hasPermission(context) || !read(context, kind).enabled) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val open = PendingIntent.getActivity(
            context,
            kind.requestCode,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(kind.label))
            .setContentText(context.getString(kind.body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        manager.notify(kind.requestCode, notification)
    }

    fun kindOf(intent: Intent): ReminderKind? = intent.getStringExtra(EXTRA_KIND)?.let { name -> ReminderKind.entries.firstOrNull { it.name == name } }
}

/** Fires a reminder, or re-arms all reminders after reboot or a clock/time-zone change. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                LogReminders.scheduleAll(context)
            else -> LogReminders.kindOf(intent)?.let { LogReminders.show(context, it) }
        }
    }
}
