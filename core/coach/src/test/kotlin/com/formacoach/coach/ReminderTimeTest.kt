package com.formacoach.coach

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
}
