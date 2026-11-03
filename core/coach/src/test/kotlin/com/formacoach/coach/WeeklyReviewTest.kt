package com.formacoach.coach

import com.formacoach.model.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class WeeklyReviewTest {
    private val today = LocalDate.of(2026, 11, 3)
    private val engine = WeeklyReviewEngine(Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneId.of("Africa/Cairo")))

    @Test fun `empty history is unknown rather than a zero calorie diet`() {
        val review = engine.review(FitnessSnapshot())
        assertNull(review.meanLoggedCalories)
        assertNull(review.meanWeight)
        assertNull(review.weightDelta)
        assertEquals(NextAction.LOG_FIRST_SESSION, review.nextAction)
    }

    @Test fun `week boundaries exclude old future and draft workouts`() {
        val sessions = listOf(
            Session("first", today.minusDays(6), "A", 1),
            Session("last", today, "B", 1),
            Session("old", today.minusDays(7), "A", 1),
            Session("future", today.plusDays(1), "A", 1),
            Session("draft", today, "A"),
        )
        val review = engine.review(FitnessSnapshot(sessions = sessions))
        assertEquals(listOf("first", "last"), review.sessionEvidence)
        assertEquals(2, review.completedSessions)
    }

    @Test fun `meal totals divide by logged days and retain evidence`() {
        val meals = listOf(
            Meal("a", today, "Meal", 500.0, 20.0, 60.0, 15.0),
            Meal("b", today, "Meal", 300.0, 10.0, 40.0, 10.0),
            Meal("c", today.minusDays(1), "Meal", 400.0, 20.0, 40.0, 10.0),
            Meal("old", today.minusDays(7), "Meal", 900.0, 20.0, 40.0, 10.0),
        )
        val review = engine.review(FitnessSnapshot(meals = meals))
        assertEquals(2, review.mealDays)
        assertEquals(600.0, review.meanLoggedCalories!!, 0.001)
        assertEquals(listOf("a", "b", "c"), review.mealEvidence)
    }

    @Test fun `weight comparison requires three observations in both windows`() {
        val measures = (0..2).map { Measurement(today.minusDays(it.toLong()), 80.0, null) } +
            (7..9).map { Measurement(today.minusDays(it.toLong()), 81.0, null) }
        val review = engine.review(FitnessSnapshot(measurements = measures))
        assertEquals(-1.0, review.weightDelta!!, 0.001)
        assertEquals(3, review.comparisonMeasurementEvidence.size)
        assertNull(engine.review(FitnessSnapshot(measurements = measures.dropLast(1))).weightDelta)
    }

    @Test fun `calendar windows follow injected zone across midnight`() {
        val localEngine = WeeklyReviewEngine(Clock.fixed(Instant.parse("2026-11-03T23:30:00Z"), ZoneOffset.ofHours(2)))
        assertEquals(LocalDate.of(2026, 11, 4), localEngine.review(FitnessSnapshot()).end)
    }

    @Test fun `waist only observations do not count as weight evidence`() {
        val review = engine.review(FitnessSnapshot(measurements = listOf(Measurement(today, null, 90.0))))
        assertEquals(0, review.weightObservations)
        assertEquals(90.0, review.meanWaist!!, 0.001)
        assertNull(review.meanWeight)
    }

    @Test fun `Arabic input supports two digit sets and rejects nonfinite values`() {
        assertEquals(82.5, NumericInput.decimal("٨٢٫٥")!!, 0.001)
        assertEquals(12, NumericInput.integer("۱۲"))
        assertNull(NumericInput.decimal("NaN"))
        assertNull(NumericInput.decimal("Infinity"))
        assertNull(NumericInput.integer("2.5"))
    }

    @Test fun `domain rejects corrupted nutrition and measurements`() {
        assertThrows(IllegalArgumentException::class.java) { Meal(date=today, name="x", calories=-1.0, protein=0.0, carbs=0.0, fat=0.0) }
        assertThrows(IllegalArgumentException::class.java) { Measurement(today, Double.NaN, null) }
        assertThrows(IllegalArgumentException::class.java) { TrainingSet(sessionId="s", exercise=Exercise.ROW, reps=0, weightKg=5.0) }
    }
}
