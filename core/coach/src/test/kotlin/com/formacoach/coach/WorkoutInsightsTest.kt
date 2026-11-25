package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class WorkoutInsightsTest {
    private val day1 = LocalDate.of(2026, 11, 20)
    private val day2 = LocalDate.of(2026, 11, 25)

    @Test fun frequentMealsRankByCountThenRecency() {
        val data = FitnessSnapshot(
            meals = listOf(
                Meal("m1", day1, "Chicken & Rice", 550.0, 45.0, 60.0, 12.0),
                Meal("m2", day2, "chicken & rice ", 550.0, 45.0, 60.0, 12.0),
                Meal("m3", day2, "Greek Yogurt", 180.0, 18.0, 12.0, 4.0),
            ),
        )
        val top = WorkoutInsights.frequentMeals(data, limit = 5)
        assertEquals(2, top.size)
        assertEquals(2, top[0].count)
        assertEquals("m2", top[0].template.id)
        assertEquals(1, top[1].count)
    }

    @Test fun formatRecapListsWorkingSetsAndExcludesWarmups() {
        val sets = listOf(
            TrainingSet("1", "s1", Exercise.BARBELL_BENCH_PRESS, 8, 80.0, SetKind.WORKING),
            TrainingSet("2", "s1", Exercise.BARBELL_BENCH_PRESS, 10, 40.0, SetKind.WARM_UP),
            TrainingSet("3", "s1", Exercise.BARBELL_ROW, 10, 70.0, SetKind.WORKING),
        )
        val text = WorkoutInsights.formatRecap(
            title = "Upper A",
            dateLabel = "2026-11-25",
            sets = sets,
            exerciseName = { it.name },
            formatLoad = { "${it.toInt()} kg" },
            workingSetsLabel = { "$it sets" },
            volumeLabel = { "${it.toInt()} kg" },
        )
        assertTrue(text.contains("Forma · Upper A (2026-11-25)"))
        assertTrue(text.contains("• BARBELL_BENCH_PRESS: 80 kg × 8"))
        assertFalse(text.contains("40 kg × 10"))
        assertTrue(text.contains("2 sets · 1340 kg"))
    }
}
