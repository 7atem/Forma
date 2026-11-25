package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class WorkoutInsightsTest {
    private val day1 = LocalDate.of(2026, 11, 20)
    private val day2 = LocalDate.of(2026, 11, 25)

    @Test fun personalRecordsExcludeWarmupsAndDraftSessions() {
        val data = FitnessSnapshot(
            sessions = listOf(
                Session("s1", day1, "A", completedAt = 1000L),
                Session("s2", day2, "A", completedAt = null), // draft
            ),
            sets = listOf(
                TrainingSet("1", "s1", Exercise.BARBELL_BENCH_PRESS, 8, 80.0, SetKind.WORKING),
                TrainingSet("2", "s1", Exercise.BARBELL_BENCH_PRESS, 5, 100.0, SetKind.WARM_UP),
                TrainingSet("3", "s2", Exercise.BARBELL_BENCH_PRESS, 5, 120.0, SetKind.WORKING),
            ),
        )
        val prs = WorkoutInsights.personalRecords(data, day2)
        assertEquals(1, prs.size)
        assertEquals(Exercise.BARBELL_BENCH_PRESS, prs[0].exercise)
        assertEquals(80.0, prs[0].weightKg, 0.001)
        assertEquals(8, prs[0].repsAtWeight)
        assertEquals(80.0 * (1.0 + 8.0 / 30.0), prs[0].bestE1rmKg, 0.01)
    }

    @Test fun weeklyMuscleVolumeFiltersEmptyAndSortsByPrimarySets() {
        val data = FitnessSnapshot(
            sessions = listOf(Session("s1", day2, "A", completedAt = 2000L)),
            sets = listOf(
                TrainingSet("1", "s1", Exercise.BARBELL_BENCH_PRESS, 8, 80.0, SetKind.WORKING),
                TrainingSet("2", "s1", Exercise.BARBELL_BENCH_PRESS, 8, 80.0, SetKind.WORKING),
                TrainingSet("3", "s1", Exercise.BARBELL_BENCH_PRESS, 10, 40.0, SetKind.WARM_UP),
            ),
        )
        val volume = WorkoutInsights.weeklyMuscleVolume(data, day2)
        assertTrue(volume.isNotEmpty())
        assertEquals(Muscle.CHEST, volume.first().muscle)
        assertEquals(2, volume.first().primarySets)
        assertTrue(volume.all { it.primarySets > 0 || it.secondarySets > 0 })
    }

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

    @Test fun summarizeSessionDetectsNewWorkingWeightPr() {
        val s1 = Session("s1", day1, "A", completedAt = 1000L)
        val s2 = Session("s2", day2, "A", completedAt = 2000L)
        val data = FitnessSnapshot(
            sessions = listOf(s1, s2),
            sets = listOf(
                TrainingSet("1", "s1", Exercise.SQUAT, 8, 100.0, SetKind.WORKING),
                TrainingSet("2", "s2", Exercise.SQUAT, 5, 110.0, SetKind.WORKING),
                TrainingSet("3", "s2", Exercise.SQUAT, 10, 60.0, SetKind.WARM_UP),
            ),
        )
        val summary = WorkoutInsights.summarizeSession(s2, data)
        assertEquals(1, summary.workingSets)
        assertEquals(1, summary.warmupSets)
        assertEquals(550.0, summary.totalVolumeKg, 0.01)
        assertEquals(listOf(Exercise.SQUAT), summary.newPrExercises)
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
