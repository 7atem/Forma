package com.formacoach.coach

import com.formacoach.model.Exercise
import com.formacoach.model.FitnessSnapshot
import java.time.LocalDate

/**
 * Facts about the last 7 local days (and 30 for weight) computed only from logged records. Days without a record
 * are unknown: averages use logged days only and nothing is inferred about adherence or health.
 */
data class ProgressHighlights(
    val workouts: Int,
    val plannedSessions: Int?,
    val cardioSeconds: Int,
    val cardioEntries: Int,
    val foodDays: Int,
    val averageKcalOnLoggedDays: Double?,
    val calorieTarget: Int?,
    val latestWeightKg: Double?,
    /** Latest minus earliest logged weight within 30 days; null with fewer than two logged dates. */
    val weightChange30Kg: Double?,
    val newRecords: List<Exercise>,
) {
    val isEmpty: Boolean get() = workouts == 0 && cardioEntries == 0 && foodDays == 0 && latestWeightKg == null

    companion object {
        fun of(data: FitnessSnapshot, today: LocalDate): ProgressHighlights {
            val week = today.minusDays(6)..today
            val workouts = data.sessions.count { it.completedAt != null && it.date in week }
            val cardio = data.cardio.filter { it.date in week }
            val kcalByDay = data.meals.filter { it.date in week }.groupBy { it.date }.mapValues { (_, meals) -> meals.sumOf { it.calories } }
            val weights = data.measurements.filter { it.weightKg != null && it.date <= today }.sortedBy { it.date }
            val month = weights.filter { it.date >= today.minusDays(29) }
            val change = if (month.size >= 2) month.last().weightKg!! - month.first().weightKg!! else null
            val records = Exercise.entries.filter { exercise ->
                MuscleTraining.best(data, exercise, today)?.date?.let { it in week } == true
            }
            return ProgressHighlights(
                workouts = workouts,
                plannedSessions = data.profile?.sessionsPerWeek,
                cardioSeconds = cardio.sumOf { it.durationSeconds },
                cardioEntries = cardio.size,
                foodDays = kcalByDay.size,
                averageKcalOnLoggedDays = kcalByDay.values.takeIf { it.isNotEmpty() }?.average(),
                calorieTarget = data.profile?.caloriesTarget,
                latestWeightKg = weights.lastOrNull()?.weightKg,
                weightChange30Kg = change,
                newRecords = records,
            )
        }
    }
}
