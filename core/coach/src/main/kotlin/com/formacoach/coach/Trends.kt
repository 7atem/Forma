package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate

/** Logged nutrition totals for one local calendar date. A day with no entries is unknown, not zero intake. */
data class NutritionDay(
    val date: LocalDate,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val entries: Int,
) {
    val logged: Boolean get() = entries > 0
}

/**
 * Descriptive comparison of logged days with the targets the user entered. Averages and target counts use
 * logged days only; unlogged days are never counted as misses. Null counts mean the user set no such target.
 */
data class NutritionTrend(
    val days: List<NutritionDay>,
    val loggedDays: Int,
    val meanCalories: Double?,
    val meanProtein: Double?,
    val meanCarbs: Double?,
    val meanFat: Double?,
    val daysCaloriesNearTarget: Int?,
    val daysProteinAtTarget: Int?,
)

object NutritionTrends {
    /** "Near" the user's own calorie target means within this fraction either side. Descriptive only. */
    const val CALORIE_BAND: Double = 0.10

    fun range(data: FitnessSnapshot, end: LocalDate, length: Int): NutritionTrend {
        require(length in 1..366)
        val start = end.minusDays((length - 1).toLong())
        val byDate = data.meals.filter { it.date in start..end }.groupBy { it.date }
        val days = (0 until length).map { offset ->
            val date = start.plusDays(offset.toLong())
            val meals = byDate[date].orEmpty()
            NutritionDay(date, meals.sumOf { it.calories }, meals.sumOf { it.protein }, meals.sumOf { it.carbs }, meals.sumOf { it.fat }, meals.size)
        }
        val logged = days.filter { it.logged }
        fun mean(selector: (NutritionDay) -> Double) = if (logged.isEmpty()) null else logged.sumOf(selector) / logged.size
        val calorieTarget = data.profile?.caloriesTarget?.toDouble()
        val proteinTarget = data.profile?.proteinTarget
        return NutritionTrend(
            days = days,
            loggedDays = logged.size,
            meanCalories = mean { it.calories },
            meanProtein = mean { it.protein },
            meanCarbs = mean { it.carbs },
            meanFat = mean { it.fat },
            daysCaloriesNearTarget = calorieTarget?.let { target ->
                logged.count { kotlin.math.abs(it.calories - target) <= target * CALORIE_BAND }
            },
            daysProteinAtTarget = proteinTarget?.let { target -> logged.count { it.protein >= target } },
        )
    }
}

/** Recorded training for one 7-day window ending on [end]. Warm-ups and unfinished sessions are excluded. */
data class TrainingWeek(
    val start: LocalDate,
    val end: LocalDate,
    val sessions: Int,
    val workingSets: Int,
    /** Sum of reps × external load in kg; bodyweight-only sets add nothing, so compare like with like. */
    val volumeKg: Double,
    val primarySetsByMuscle: Map<Muscle, Int>,
)

object TrainingTrends {
    /** Consecutive 7-day windows ending on [end], oldest first, so weeks never depend on a locale's first weekday. */
    fun weekly(data: FitnessSnapshot, end: LocalDate, weeks: Int): List<TrainingWeek> {
        require(weeks in 1..104)
        val completed = data.sessions.filter { it.completedAt != null && it.date <= end }
        val setsBySession = data.sets.filter { it.kind == SetKind.WORKING }.groupBy { it.sessionId }
        return (weeks - 1 downTo 0).map { back ->
            val weekEnd = end.minusDays(7L * back)
            val weekStart = weekEnd.minusDays(6)
            val inWeek = completed.filter { it.date in weekStart..weekEnd }
            val sets = inWeek.flatMap { setsBySession[it.id].orEmpty() }
            val byMuscle = mutableMapOf<Muscle, Int>()
            sets.forEach { set -> ExerciseMuscles.of(set.exercise).primary.forEach { byMuscle[it] = (byMuscle[it] ?: 0) + 1 } }
            TrainingWeek(weekStart, weekEnd, inWeek.size, sets.size, sets.sumOf { it.reps * it.weightKg }, byMuscle)
        }
    }
}
