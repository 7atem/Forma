package com.formacoach.coach

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Pure daily-reminder timing and selection so scheduling can be unit-tested without Android alarms. */
object ReminderTime {
    const val MINUTES_PER_DAY: Int = 24 * 60

    /** Bit (dayOfWeek.value - 1) set = remind on that weekday. */
    const val EVERY_DAY: Int = 0b1111111

    /** The next local date-time at [minuteOfDay]; if that time has passed today (or is now), tomorrow. */
    fun next(now: LocalDateTime, minuteOfDay: Int): LocalDateTime {
        require(minuteOfDay in 0 until MINUTES_PER_DAY)
        val today = now.toLocalDate().atTime(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    fun dayBit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun isActiveDay(date: LocalDate, dayMask: Int): Boolean = dayMask and dayBit(date.dayOfWeek) != 0

    /**
     * Whether a reminder should be shown on [date]. A reminder is skipped only when the person chose to skip
     * already-logged days and a record for that day is known to exist; an unreadable log ([loggedToday] null)
     * still shows the reminder rather than guessing.
     */
    fun shouldNotify(date: LocalDate, dayMask: Int, skipIfLogged: Boolean, loggedToday: Boolean?): Boolean =
        isActiveDay(date, dayMask) && !(skipIfLogged && loggedToday == true)

    /** Rotates through [count] message variants by local date, so the same day always gets the same text. */
    fun variant(date: LocalDate, count: Int): Int {
        require(count > 0)
        return Math.floorMod(date.toEpochDay(), count.toLong()).toInt()
    }
}
