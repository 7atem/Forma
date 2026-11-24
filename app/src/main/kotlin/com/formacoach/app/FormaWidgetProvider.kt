package com.formacoach.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.formacoach.model.FitnessSnapshot
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Lightweight, read-only Android Home Screen widget showing today's logged calories, protein,
 * and 7-day completed workouts without opening the encrypted database from a background broadcast.
 */
class FormaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    companion object {
        private const val PREFS = "forma_widget_summary"

        fun updateSummary(context: Context, snapshot: FitnessSnapshot, today: LocalDate) {
            val todayMeals = snapshot.meals.filter { it.date == today }
            val kcal = todayMeals.sumOf { it.calories }.roundToInt()
            val protein = todayMeals.sumOf { it.protein }.roundToInt()
            val weekStart = today.minusDays(6)
            val weekSessions = snapshot.sessions.count { it.completedAt != null && it.date in weekStart..today }
            val targetSessions = snapshot.profile?.sessionsPerWeek ?: 3
            val hasActive = snapshot.sessions.any { it.completedAt == null }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong("date_epoch", today.toEpochDay())
                .putInt("kcal", kcal)
                .putInt("protein", protein)
                .putInt("week_sessions", weekSessions)
                .putInt("target_sessions", targetSessions)
                .putBoolean("has_active", hasActive)
                .apply()
            refresh(context)
        }

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, FormaWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = buildViews(context)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun buildViews(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val storedDay = prefs.getLong("date_epoch", -1L)
            val isToday = storedDay == LocalDate.now().toEpochDay()
            val kcal = if (isToday) prefs.getInt("kcal", 0) else 0
            val protein = if (isToday) prefs.getInt("protein", 0) else 0
            val weekSessions = prefs.getInt("week_sessions", 0)
            val targetSessions = prefs.getInt("target_sessions", 3)
            val hasActive = prefs.getBoolean("has_active", false)

            val openIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            return RemoteViews(context.packageName, R.layout.forma_widget).apply {
                setOnClickPendingIntent(R.id.widget_root, openIntent)
                setTextViewText(R.id.widget_kcal_value, kcal.toString())
                setTextViewText(R.id.widget_protein_value, context.getString(R.string.grams_amount, protein.toString()))
                setTextViewText(R.id.widget_week_value, "$weekSessions/$targetSessions")
                setTextViewText(
                    R.id.widget_status,
                    context.getString(if (hasActive) R.string.resume_session else R.string.widget_today_label),
                )
            }
        }
    }
}
