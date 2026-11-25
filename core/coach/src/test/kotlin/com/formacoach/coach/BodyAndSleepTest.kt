package com.formacoach.coach

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class BodyAndSleepTest {
    private fun at(day: Int, hour: Int, minute: Int = 0) = Instant.parse("2026-11-%02dT%02d:%02d:00Z".format(day, hour, minute))

    @Test fun nightsUseOneSourceAndWakeDate() {
        val segments = listOf(
            SleepSegment(at(24, 23), at(25, 2), SleepStage.CORE, "watch"),
            SleepSegment(at(25, 2), at(25, 3), SleepStage.DEEP, "watch"),
            SleepSegment(at(25, 3), at(25, 3, 20), SleepStage.AWAKE, "watch"),
            SleepSegment(at(25, 3, 20), at(25, 6), SleepStage.REM, "watch"),
            SleepSegment(at(24, 23, 30), at(25, 6, 30), SleepStage.ASLEEP, "phone"), // overlapping estimate
            SleepSegment(at(25, 23), at(25, 22), SleepStage.ASLEEP, "bad"),
        )
        val night = SleepNights.summarize(segments, ZoneOffset.UTC).single()
        assertEquals(LocalDate.of(2026, 11, 25), night.date)
        assertEquals("phone", night.source)
        assertEquals(420.0, night.asleepMinutes, 0.01)
        val watch = SleepNights.summarize(segments.take(4), ZoneOffset.UTC).single()
        assertEquals(400.0, watch.asleepMinutes, 0.01)
        assertEquals(60.0, watch.deepMinutes!!, 0.01)
        assertEquals(20.0, watch.awakeMinutes!!, 0.01)
    }

    @Test fun bmiIsBounded() {
        assertEquals(24.69, BodyMetrics.bmi(80.0, 180.0)!!, 0.01)
        assertNull(BodyMetrics.bmi(80.0, 20.0))
        assertNull(BodyMetrics.bmi(0.0, 180.0))
        assertNull(BodyMetrics.bmi(Double.NaN, 180.0))
    }
}
