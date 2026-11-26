package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressHighlightsTest {
    private val today = LocalDate.of(2026, 11, 26)

    @Test fun emptyLogIsEmptyAndUnknownNotZero() {
        val h = ProgressHighlights.of(FitnessSnapshot(profile = Profile("", Goal.BUILD_HABIT, 3)), today)
        assertTrue(h.isEmpty)
        assertNull(h.averageKcalOnLoggedDays)
        assertNull(h.weightChange30Kg)
        assertEquals(3, h.plannedSessions)
    }

    @Test fun countsOnlyTheLastSevenLoggedDays() {
        val inWeek = Session(id = "a", date = today.minusDays(2), templateId = "x", completedAt = 1, label = "", plannedExercises = listOf(Exercise.SQUAT), restSeconds = 90)
        val old = inWeek.copy(id = "b", date = today.minusDays(8))
        val draft = inWeek.copy(id = "c", completedAt = null)
        val sets = listOf(
            TrainingSet(id = "s1", sessionId = "a", exercise = Exercise.SQUAT, reps = 5, weightKg = 100.0),
            TrainingSet(id = "s2", sessionId = "b", exercise = Exercise.SQUAT, reps = 5, weightKg = 90.0),
            TrainingSet(id = "s3", sessionId = "b", exercise = Exercise.ROW, reps = 8, weightKg = 50.0),
        )
        val meals = listOf(
            Meal(date = today, name = "a", calories = 2000.0, protein = 1.0, carbs = 1.0, fat = 1.0),
            Meal(date = today, name = "b", calories = 400.0, protein = 1.0, carbs = 1.0, fat = 1.0),
            Meal(date = today.minusDays(1), name = "c", calories = 1600.0, protein = 1.0, carbs = 1.0, fat = 1.0),
            Meal(date = today.minusDays(9), name = "old", calories = 9000.0, protein = 1.0, carbs = 1.0, fat = 1.0),
        )
        val cardio = listOf(
            CardioEntry(date = today, activity = CardioActivity.RUN, durationSeconds = 1800),
            CardioEntry(date = today.minusDays(7), activity = CardioActivity.RUN, durationSeconds = 600),
        )
        val weights = listOf(
            Measurement(today.minusDays(40), 90.0, null),
            Measurement(today.minusDays(20), 84.0, null),
            Measurement(today.minusDays(1), 82.5, null),
        )
        val h = ProgressHighlights.of(FitnessSnapshot(sessions = listOf(inWeek, old, draft), sets = sets, meals = meals, cardio = cardio, measurements = weights), today)
        assertEquals(1, h.workouts)
        assertEquals(1800, h.cardioSeconds)
        assertEquals(1, h.cardioEntries)
        assertEquals(2, h.foodDays)
        assertEquals(2000.0, h.averageKcalOnLoggedDays!!, 0.001) // (2400 + 1600) / 2 logged days
        assertEquals(82.5, h.latestWeightKg!!, 0.001)
        assertEquals(-1.5, h.weightChange30Kg!!, 0.001) // only the two entries inside 30 days
        assertEquals(listOf(Exercise.SQUAT), h.newRecords) // the row best is older than 7 days
    }
}
