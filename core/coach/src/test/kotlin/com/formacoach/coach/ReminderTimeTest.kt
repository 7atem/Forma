package com.formacoach.coach

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTimeTest {
    @Test fun nextIsLaterTodayOrTomorrowNeverNow() {
        val now = LocalDateTime.of(2026, 11, 25, 12, 30)
        assertEquals(LocalDateTime.of(2026, 11, 25, 13, 0), ReminderTime.next(now, 13 * 60))
        assertEquals(LocalDateTime.of(2026, 11, 26, 8, 0), ReminderTime.next(now, 8 * 60))
        assertEquals(LocalDateTime.of(2026, 11, 26, 12, 30), ReminderTime.next(now, 12 * 60 + 30))
        assertEquals(LocalDateTime.of(2026, 12, 1, 0, 0), ReminderTime.next(LocalDateTime.of(2026, 11, 30, 23, 59), 0))
        assertThrows(IllegalArgumentException::class.java) { ReminderTime.next(now, 24 * 60) }
    }

    @Test fun onlyChosenWeekdaysAreActive() {
        val wednesday = LocalDate.of(2026, 11, 25)
        assertEquals(DayOfWeek.WEDNESDAY, wednesday.dayOfWeek)
        val monWedFri = ReminderTime.dayBit(DayOfWeek.MONDAY) or ReminderTime.dayBit(DayOfWeek.WEDNESDAY) or ReminderTime.dayBit(DayOfWeek.FRIDAY)
        assertTrue(ReminderTime.isActiveDay(wednesday, monWedFri))
        assertFalse(ReminderTime.isActiveDay(wednesday.plusDays(1), monWedFri))
        assertTrue((0..6).all { ReminderTime.isActiveDay(wednesday.plusDays(it.toLong()), ReminderTime.EVERY_DAY) })
        assertFalse(ReminderTime.isActiveDay(wednesday, 0))
    }

    @Test fun skipsOnlyWhenChosenAndALogIsKnown() {
        val day = LocalDate.of(2026, 11, 25)
        assertTrue(ReminderTime.shouldNotify(day, ReminderTime.EVERY_DAY, skipIfLogged = true, loggedToday = false))
        assertFalse(ReminderTime.shouldNotify(day, ReminderTime.EVERY_DAY, skipIfLogged = true, loggedToday = true))
        assertTrue(ReminderTime.shouldNotify(day, ReminderTime.EVERY_DAY, skipIfLogged = false, loggedToday = true))
        // An unreadable log is unknown, not evidence either way: remind.
        assertTrue(ReminderTime.shouldNotify(day, ReminderTime.EVERY_DAY, skipIfLogged = true, loggedToday = null))
        assertFalse(ReminderTime.shouldNotify(day, 0, skipIfLogged = false, loggedToday = false))
    }

    @Test fun variantRotatesDailyAndIsStableForADay() {
        val day = LocalDate.of(2026, 11, 25)
        val week = (0..3).map { ReminderTime.variant(day.plusDays(it.toLong()), 4) }
        assertEquals(setOf(0, 1, 2, 3), week.toSet())
        assertEquals(ReminderTime.variant(day, 4), ReminderTime.variant(day, 4))
        assertEquals(0, ReminderTime.variant(LocalDate.of(1969, 12, 28), 4))
        assertThrows(IllegalArgumentException::class.java) { ReminderTime.variant(day, 0) }
    }
}
