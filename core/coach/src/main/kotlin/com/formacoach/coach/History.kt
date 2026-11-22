package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate

data class HistoryPoint(val date: LocalDate, val value: Double)

object TrainingHistory {
    /** Epley estimated 1RM for working sets with external load and 1–15 reps. Warm-ups are excluded by callers. */
    fun estimatedOneRepMax(weightKg: Double, reps: Int): Double? {
        if (!weightKg.isFinite() || weightKg <= 0.0 || reps !in 1..15) return null
        return if (reps == 1) weightKg else weightKg * (1.0 + reps / 30.0)
    }

    fun loads(data: FitnessSnapshot, exercise: Exercise, asOf: LocalDate): List<HistoryPoint> {
        val sessions = data.sessions.filter { it.completedAt != null && it.date <= asOf }.associateBy { it.id }
        return data.sets.filter { it.exercise == exercise && it.kind == SetKind.WORKING && it.sessionId in sessions }
            .groupBy { sessions.getValue(it.sessionId).date }
            .map { (date, sets) -> HistoryPoint(date, sets.maxOf { it.weightKg }) }
            .sortedBy { it.date }
    }

    fun e1rm(data: FitnessSnapshot, exercise: Exercise, asOf: LocalDate): List<HistoryPoint> {
        val sessions = data.sessions.filter { it.completedAt != null && it.date <= asOf }.associateBy { it.id }
        return data.sets.filter { it.exercise == exercise && it.kind == SetKind.WORKING && it.sessionId in sessions }
            .groupBy { sessions.getValue(it.sessionId).date }
            .mapNotNull { (date, sets) ->
                sets.mapNotNull { estimatedOneRepMax(it.weightKg, it.reps) }.maxOrNull()?.let { HistoryPoint(date, it) }
            }
            .sortedBy { it.date }
    }

    fun bestE1rm(data: FitnessSnapshot, exercise: Exercise, asOf: LocalDate): HistoryPoint? =
        e1rm(data, exercise, asOf).maxByOrNull { it.value }

    /** Total working-set volume load (reps * external load kg). Warm-up sets are excluded. */
    fun sessionTonnage(sets: List<TrainingSet>): Double =
        sets.filter { it.kind == SetKind.WORKING }.sumOf { it.reps * it.weightKg }

    fun weeklyTonnage(data: FitnessSnapshot, asOf: LocalDate): Double {
        val start = asOf.minusDays(6)
        val completedIds = data.sessions.filter { it.completedAt != null && it.date in start..asOf }.map { it.id }.toSet()
        return sessionTonnage(data.sets.filter { it.sessionId in completedIds })
    }

    /** True when there is prior completed history for this exercise and the set beats prior best weight or e1RM. */
    fun isNewPr(data: FitnessSnapshot, exercise: Exercise, weightKg: Double, reps: Int?, asOf: LocalDate): Boolean {
        if (!weightKg.isFinite() || weightKg <= 0.0) return false
        val completedIds = data.sessions.filter { it.completedAt != null && it.date <= asOf }.map { it.id }.toSet()
        val prior = data.sets.filter { it.exercise == exercise && it.kind == SetKind.WORKING && it.sessionId in completedIds }
        if (prior.isEmpty()) return false
        val priorBestWeight = prior.maxOf { it.weightKg }
        if (weightKg > priorBestWeight) return true
        val candidateE1rm = reps?.let { estimatedOneRepMax(weightKg, it) } ?: return false
        val priorBestE1rm = prior.mapNotNull { estimatedOneRepMax(it.weightKg, it.reps) }.maxOrNull() ?: return false
        return candidateE1rm > priorBestE1rm + 0.01
    }

    /** Consecutive 7-day rolling windows ending at asOf with at least one completed workout. */
    fun activeWeekStreak(data: FitnessSnapshot, asOf: LocalDate): Int {
        val dates = data.sessions.filter { it.completedAt != null && it.date <= asOf }.map { it.date }.toSet()
        var streak = 0
        while (streak < 52) {
            val end = asOf.minusDays(streak * 7L)
            val start = end.minusDays(6)
            if (dates.any { it in start..end }) streak++ else break
        }
        return streak
    }

    fun macroCalories(protein: Double, carbs: Double, fat: Double): Double =
        (protein.coerceAtLeast(0.0) * 4.0) + (carbs.coerceAtLeast(0.0) * 4.0) + (fat.coerceAtLeast(0.0) * 9.0)

    fun weights(data: FitnessSnapshot, asOf: LocalDate): List<HistoryPoint> = data.measurements
        .filter { it.date in asOf.minusDays(89)..asOf && it.weightKg != null }
        .sortedBy { it.date }.map { HistoryPoint(it.date, it.weightKg!!) }

    /** Best recorded working load per rep target (1, 3, 5, 8, 10, 12 reps). Warm-ups excluded. */
    fun repMaxLadder(data: FitnessSnapshot, exercise: Exercise): List<Pair<Int, Double>> {
        val completedIds = data.sessions.filter { it.completedAt != null }.map { it.id }.toSet()
        val working = data.sets.filter {
            it.sessionId in completedIds && it.exercise == exercise && it.kind == SetKind.WORKING && it.weightKg > 0.0
        }
        return listOf(1, 3, 5, 8, 10, 12).mapNotNull { repsTarget ->
            val best = working.filter { it.reps >= repsTarget }.maxOfOrNull { it.weightKg }
            best?.let { repsTarget to it }
        }
    }
}

object RestTimer {
    fun remainingSeconds(deadlineElapsedMillis: Long, nowElapsedMillis: Long, durationSeconds: Int): Long {
        if (deadlineElapsedMillis <= nowElapsedMillis || durationSeconds !in 15..600) return 0
        val delta = deadlineElapsedMillis - nowElapsedMillis
        // Monotonic time resets on reboot; a saved deadline must never become a multi-day timer.
        if (delta < 0 || delta > durationSeconds * 1000L) return 0
        return (delta + 999) / 1000
    }
}

enum class CoachQuestion { WEEK, TRAINING, FOOD, WEIGHT, UNSUPPORTED }

object CoachQuestions {
    fun resolve(text: String): CoachQuestion {
        val value = text.lowercase().trim().take(500)
        return when {
            listOf("weight", "وزن").any(value::contains) -> CoachQuestion.WEIGHT
            listOf("food", "calor", "protein", "أكل", "اكل", "سعر", "بروتين").any(value::contains) -> CoachQuestion.FOOD
            listOf("train", "workout", "تمرين", "تمار").any(value::contains) -> CoachQuestion.TRAINING
            listOf("week", "progress", "أسبوع", "اسبوع", "تقدم").any(value::contains) -> CoachQuestion.WEEK
            else -> CoachQuestion.UNSUPPORTED
        }
    }
}
