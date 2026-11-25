package com.formacoach.coach

import java.time.LocalDateTime
import java.time.LocalTime

/** Pure daily-reminder timing so scheduling can be unit-tested without Android alarms. */
object ReminderTime {
    const val MINUTES_PER_DAY: Int = 24 * 60

    /** The next local date-time at [minuteOfDay]; if that time has passed today (or is now), tomorrow. */
    fun next(now: LocalDateTime, minuteOfDay: Int): LocalDateTime {
        require(minuteOfDay in 0 until MINUTES_PER_DAY)
        val today = now.toLocalDate().atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
        return if (today.isAfter(now)) today else today.plusDays(1)
    }
}
