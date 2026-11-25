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
import com.formacoach.data.FitnessRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Optional reminders. Off by default; the text is encouraging and never includes logged data.
 * [tab] is the screen a tap opens: 1 Train, 2 Eat, 3 Progress.
 */
internal enum class ReminderKind(
    val requestCode: Int,
    val defaultMinute: Int,
    val label: Int,
    val title: Int,
    val bodies: List<Int>,
    val tab: Int,
    val checksLog: Boolean,
) {
    MEALS(
        5101, 13 * 60, R.string.reminder_meals, R.string.reminder_meals_title,
        listOf(R.string.reminder_meals_body_1, R.string.reminder_meals_body_2, R.string.reminder_meals_body_3, R.string.reminder_meals_body_4),
        tab = 2, checksLog = true,
    ),
    WATER(
        5102, 11 * 60, R.string.reminder_water, R.string.reminder_water_title,
        listOf(R.string.reminder_water_body_1, R.string.reminder_water_body_2, R.string.reminder_water_body_3, R.string.reminder_water_body_4),
        tab = 2, checksLog = true,
    ),
    WORKOUT(
        5103, 18 * 60, R.string.reminder_workout, R.string.reminder_workout_title,
        listOf(R.string.reminder_workout_body_1, R.string.reminder_workout_body_2, R.string.reminder_workout_body_3, R.string.reminder_workout_body_4),
        tab = 1, checksLog = true,
    ),
    WEEKLY(
        5104, 19 * 60, R.string.reminder_weekly, R.string.reminder_weekly_title,
        listOf(R.string.reminder_weekly_body_1, R.string.reminder_weekly_body_2),
        tab = 3, checksLog = false,
    ),
    ;

    val channelId: String get() = "forma_reminder_${name.lowercase()}"

    /** Daily kinds default to every day; the weekly look-back defaults to the last day of the local week. */
    fun defaultDays(locale: Locale): Int = if (this == WEEKLY) {
        ReminderTime.dayBit(WeekFields.of(locale).firstDayOfWeek.minus(1))
    } else {
        ReminderTime.EVERY_DAY
    }
}

internal data class ReminderSetting(val enabled: Boolean, val minuteOfDay: Int, val days: Int)

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ReminderEntryPoint {
    fun repository(): FitnessRepository
}

internal object LogReminders {
    private const val PREFS = "forma_ui_prefs"
    private const val LEGACY_CHANNEL_ID = "forma_log_reminders"
    private const val EXTRA_KIND = "forma_reminder_kind"
    const val EXTRA_OPEN_TAB = "forma_open_tab"
    const val ACTION_ADD_WATER = "com.formacoach.app.REMINDER_ADD_WATER"
    const val ACTION_SNOOZE = "com.formacoach.app.REMINDER_SNOOZE"
    private const val ACTION_SNOOZED = "com.formacoach.app.REMINDER_SNOOZED"
    const val WATER_STEP_ML = 250
    private const val SNOOZE_MS = 60 * 60 * 1000L
    private const val SKIP_KEY = "reminders_skip_logged"

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(context: Context, kind: ReminderKind): ReminderSetting {
        val prefs = prefs(context)
        val minute = prefs.getInt("reminder_${kind.name}_minute", kind.defaultMinute).coerceIn(0, ReminderTime.MINUTES_PER_DAY - 1)
        val days = prefs.getInt("reminder_${kind.name}_days", kind.defaultDays(Locale.getDefault())) and ReminderTime.EVERY_DAY
        return ReminderSetting(prefs.getBoolean("reminder_${kind.name}_on", false), minute, days.takeIf { it != 0 } ?: ReminderTime.EVERY_DAY)
    }

    fun save(context: Context, kind: ReminderKind, setting: ReminderSetting) {
        prefs(context).edit()
            .putBoolean("reminder_${kind.name}_on", setting.enabled)
            .putInt("reminder_${kind.name}_minute", setting.minuteOfDay)
            .putInt("reminder_${kind.name}_days", setting.days)
            .apply()
        schedule(context, kind, setting)
    }

    /** On by default: a reminder is not shown when that day's log already has a record of its kind. */
    fun skipIfLogged(context: Context): Boolean = prefs(context).getBoolean(SKIP_KEY, true)

    fun setSkipIfLogged(context: Context, value: Boolean) = prefs(context).edit().putBoolean(SKIP_KEY, value).apply()

    /** Re-arms every enabled reminder; safe to call repeatedly (app start, reboot, time-zone change). */
    fun scheduleAll(context: Context) = ReminderKind.entries.forEach { schedule(context, it, read(context, it)) }

    private fun broadcast(context: Context, kind: ReminderKind, action: String?, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, ReminderAlarmReceiver::class.java).setAction(action).putExtra(EXTRA_KIND, kind.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun schedule(context: Context, kind: ReminderKind, setting: ReminderSetting) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = broadcast(context, kind, null, kind.requestCode)
        alarms.cancel(intent)
        // Earlier builds aimed the daily alarm at the exported receiver; retire that alarm.
        PendingIntent.getBroadcast(
            context,
            kind.requestCode,
            Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_KIND, kind.name),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )?.let { alarms.cancel(it); it.cancel() }
        if (!setting.enabled) {
            alarms.cancel(broadcast(context, kind, ACTION_SNOOZED, kind.requestCode + 100))
            return
        }
        val zone = ZoneId.systemDefault()
        val next = ReminderTime.next(LocalDateTime.now(zone), setting.minuteOfDay).atZone(zone).toInstant().toEpochMilli()
        // Daily and inexact on purpose: no exact-alarm permission, and the system may batch it to save battery.
        // Unchosen weekdays are filtered when the alarm fires.
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, next, AlarmManager.INTERVAL_DAY, intent)
    }

    private fun snooze(context: Context, kind: ReminderKind) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + SNOOZE_MS, broadcast(context, kind, ACTION_SNOOZED, kind.requestCode + 100))
    }

    private fun ensureChannels(context: Context, manager: NotificationManager) {
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        ReminderKind.entries.forEach { kind ->
            manager.createNotificationChannel(
                NotificationChannel(kind.channelId, context.getString(kind.label), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }

    private suspend fun loggedOn(repository: FitnessRepository, kind: ReminderKind, date: LocalDate): Boolean = when (kind) {
        ReminderKind.MEALS -> repository.hasMealsOn(date)
        ReminderKind.WATER -> repository.hasHydrationOn(date)
        ReminderKind.WORKOUT -> repository.hasTrainingOn(date)
        ReminderKind.WEEKLY -> false
    }

    /** Called when an alarm fires. Checks the chosen days and, if asked, whether today already has a record. */
    suspend fun fire(context: Context, kind: ReminderKind) {
        val setting = read(context, kind)
        if (!hasPermission(context) || !setting.enabled) return
        val today = LocalDate.now()
        val skip = kind.checksLog && skipIfLogged(context)
        // An unreadable log is unknown, not proof of anything: the reminder is shown rather than guessed away.
        val logged = if (!skip || !ReminderTime.isActiveDay(today, setting.days)) null else runCatching {
            withTimeoutOrNull(5_000) { loggedOn(repository(context), kind, today) }
        }.getOrNull()
        if (ReminderTime.shouldNotify(today, setting.days, skip, logged)) post(context, kind, today)
    }

    /** "Send a test": shows today's message now, whatever the switch, days or log say. */
    fun sendTest(context: Context, kind: ReminderKind) {
        if (hasPermission(context)) post(context, kind, LocalDate.now())
    }

    fun message(context: Context, kind: ReminderKind, date: LocalDate): String =
        context.getString(kind.bodies[ReminderTime.variant(date, kind.bodies.size)])

    private fun post(context: Context, kind: ReminderKind, date: LocalDate) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannels(context, manager)
        val open = PendingIntent.getActivity(
            context,
            kind.requestCode,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_TAB, kind.tab)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = message(context, kind, date)
        val builder = NotificationCompat.Builder(context, kind.channelId)
            .setSmallIcon(R.drawable.ic_stat_forma)
            .setColor(0xFF2E6B24.toInt())
            .setContentTitle(context.getString(kind.title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (kind == ReminderKind.WATER) {
            builder.addAction(0, context.getString(R.string.water_add, WATER_STEP_ML.toString()), broadcast(context, kind, ACTION_ADD_WATER, kind.requestCode + 200))
        }
        builder.addAction(0, context.getString(R.string.reminder_action_snooze), broadcast(context, kind, ACTION_SNOOZE, kind.requestCode + 300))
        manager.notify(kind.requestCode, builder.build())
    }

    /** The "+250 ml" button: adds water to today's log after the person tapped it, then confirms briefly. */
    suspend fun addWaterFromNotification(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val saved = runCatching { repository(context).addHydration(LocalDate.now(), WATER_STEP_ML) }.isSuccess
        val confirm = NotificationCompat.Builder(context, ReminderKind.WATER.channelId)
            .setSmallIcon(R.drawable.ic_stat_forma)
            .setColor(0xFF2E6B24.toInt())
            .setContentTitle(context.getString(if (saved) R.string.reminder_water_added else R.string.reminder_water_failed))
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setTimeoutAfter(4_000)
            .setAutoCancel(true)
            .build()
        manager.notify(ReminderKind.WATER.requestCode, confirm)
    }

    fun handleSnooze(context: Context, kind: ReminderKind) {
        context.getSystemService(NotificationManager::class.java)?.cancel(kind.requestCode)
        snooze(context, kind)
    }

    private fun repository(context: Context): FitnessRepository =
        EntryPointAccessors.fromApplication(context.applicationContext, ReminderEntryPoint::class.java).repository()

    fun kindOf(intent: Intent): ReminderKind? = intent.getStringExtra(EXTRA_KIND)?.let { name -> ReminderKind.entries.firstOrNull { it.name == name } }

    /** Weekdays in the order of the local week, for the day chips. */
    fun weekOrder(locale: Locale): List<DayOfWeek> {
        val first = WeekFields.of(locale).firstDayOfWeek
        return (0L until 7L).map { first.plus(it) }
    }
}

/** Re-arms all reminders after reboot or a clock/time-zone change. Exported for system broadcasts only. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                LogReminders.scheduleAll(context)
        }
    }
}

/**
 * Fires a reminder and handles its buttons. Not exported: only Forma's own alarms and notification
 * buttons can reach it, so no other app can add water or trigger reminders.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            LogReminders.ACTION_SNOOZE -> LogReminders.kindOf(intent)?.let { LogReminders.handleSnooze(context, it) }
            LogReminders.ACTION_ADD_WATER -> async { LogReminders.addWaterFromNotification(context) }
            else -> LogReminders.kindOf(intent)?.let { kind -> async { LogReminders.fire(context, kind) } }
        }
    }

    /** Short database work off the main thread; the receiver stays alive until it finishes. */
    private fun async(block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }
}
