package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TrendsTest {
    private val end = LocalDate.of(2026, 11, 25)

    private fun meal(date: LocalDate, kcal: Double, protein: Double) =
        Meal(date = date, name = "m", calories = kcal, protein = protein, carbs = 10.0, fat = 5.0)

    @Test fun unloggedDaysAreUnknownAndTargetsCountOnlyLoggedDays() {
        val data = FitnessSnapshot(
            profile = Profile("", Goal.BUILD_HABIT, 3, caloriesTarget = 2000, proteinTarget = 120.0),
            meals = listOf(
                meal(end, 1000.0, 60.0), meal(end, 1050.0, 70.0), // 2050 kcal, 130 g: near target, protein met
                meal(end.minusDays(2), 1500.0, 100.0),            // 1500 kcal: outside the 10% band
                meal(end.minusDays(7), 9999.0, 999.0),            // outside the 7-day window
                meal(end.plusDays(1), 9999.0, 999.0),             // future date ignored
            ),
        )
        val trend = NutritionTrends.range(data, end, 7)
        assertEquals(7, trend.days.size)
        assertEquals(end.minusDays(6), trend.days.first().date)
        assertEquals(2, trend.loggedDays)
        assertEquals((2050.0 + 1500.0) / 2, trend.meanCalories!!, 0.001)
        assertEquals(1, trend.daysCaloriesNearTarget)
        assertEquals(1, trend.daysProteinAtTarget)
        assertFalse(trend.days[3].logged)
        assertEquals(2, trend.days.last().entries)
    }

    @Test fun noTargetsMeansNoTargetCountsAndNoLogsMeansNoAverages() {
        val trend = NutritionTrends.range(FitnessSnapshot(), end, 30)
        assertEquals(30, trend.days.size)
        assertEquals(0, trend.loggedDays)
        assertNull(trend.meanCalories)
        assertNull(trend.daysCaloriesNearTarget)
        assertNull(trend.daysProteinAtTarget)
    }

    @Test fun weeklyTrainingExcludesWarmupsDraftsAndFutureAndUsesRollingWeeks() {
        val done = Session("done", end.minusDays(1), "A", 1)
        val lastWeek = Session("last", end.minusDays(8), "A", 1)
        val draft = Session("draft", end, "A")
        val future = Session("future", end.plusDays(2), "A", 1)
        val sets = listOf(
            TrainingSet(sessionId = "done", exercise = Exercise.SQUAT, reps = 10, weightKg = 40.0),
            TrainingSet(sessionId = "done", exercise = Exercise.SQUAT, reps = 5, weightKg = 20.0, kind = SetKind.WARM_UP),
            TrainingSet(sessionId = "last", exercise = Exercise.ROW, reps = 8, weightKg = 30.0),
            TrainingSet(sessionId = "draft", exercise = Exercise.ROW, reps = 8, weightKg = 30.0),
            TrainingSet(sessionId = "future", exercise = Exercise.ROW, reps = 8, weightKg = 30.0),
        )
        val weeks = TrainingTrends.weekly(FitnessSnapshot(sessions = listOf(done, lastWeek, draft, future), sets = sets), end, 3)
        assertEquals(3, weeks.size)
        val current = weeks.last()
        assertEquals(end.minusDays(6), current.start)
        assertEquals(1, current.sessions)
        assertEquals(1, current.workingSets)
        assertEquals(400.0, current.volumeKg, 0.0)
        assertEquals(1, current.primarySetsByMuscle[Muscle.QUADRICEPS])
        assertEquals(240.0, weeks[1].volumeKg, 0.0)
        assertEquals(0, weeks.first().workingSets)
    }
}
