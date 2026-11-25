package com.formacoach.coach

import com.formacoach.model.*
import java.time.LocalDate

/** All-time personal record for one exercise across completed working sets. */
data class PersonalRecord(
    val exercise: Exercise,
    val weightKg: Double,
    val repsAtWeight: Int,
    val bestE1rmKg: Double,
    val date: LocalDate,
)

/** One frequently logged meal template for 1-tap quick re-logging. */
data class FrequentMeal(
    val template: Meal,
    val count: Int,
)

/** Deterministic summary of a workout session, including exercises where a new working-weight PR was hit. */
data class SessionSummary(
    val workingSets: Int,
    val warmupSets: Int,
    val totalVolumeKg: Double,
    val exercisesCount: Int,
    val primaryMuscles: List<Muscle>,
    val newPrExercises: List<Exercise>,
)

object WorkoutInsights {
    /** Epley estimated 1RM for working sets (returns weightKg directly for 1-rep sets). */
    fun e1rm(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        return if (reps == 1) weightKg else weightKg * (1.0 + reps / 30.0)
    }

    /** Personal records across all completed sessions up to [asOf], sorted by most recent PR date then highest weight. */
    fun personalRecords(data: FitnessSnapshot, asOf: LocalDate): List<PersonalRecord> {
        val completed = data.sessions.filter { it.completedAt != null && it.date <= asOf }.associateBy { it.id }
        if (completed.isEmpty()) return emptyList()
        return data.sets
            .filter { it.kind == SetKind.WORKING && it.sessionId in completed }
            .groupBy { it.exercise }
            .mapNotNull { (exercise, sets) ->
                val bestLoad = sets.maxWithOrNull(compareBy<TrainingSet> { it.weightKg }.thenBy { it.reps }) ?: return@mapNotNull null
                val bestE1rm = sets.maxOf { e1rm(it.weightKg, it.reps) }
                val date = completed.getValue(bestLoad.sessionId).date
                PersonalRecord(
                    exercise = exercise,
                    weightKg = bestLoad.weightKg,
                    repsAtWeight = bestLoad.reps,
                    bestE1rmKg = bestE1rm,
                    date = date,
                )
            }
            .sortedWith(compareByDescending<PersonalRecord> { it.date }.thenByDescending { it.weightKg })
    }

    /** Last-7-day working-set breakdown per muscle, excluding untouched muscles and sorted by primary then supporting sets. */
    fun weeklyMuscleVolume(data: FitnessSnapshot, asOf: LocalDate): List<MuscleSetCount> =
        MuscleTraining.weekly(data, asOf)
            .filter { it.primarySets > 0 || it.secondarySets > 0 }
            .sortedWith(compareByDescending<MuscleSetCount> { it.primarySets }.thenByDescending { it.secondarySets })

    /** Most frequently logged meals (case-insensitive trimmed name + rounded calories), newest entry kept as template. */
    fun frequentMeals(data: FitnessSnapshot, limit: Int = 8): List<FrequentMeal> {
        if (limit <= 0 || data.meals.isEmpty()) return emptyList()
        return data.meals
            .groupBy { meal ->
                meal.name.trim().lowercase() to kotlin.math.round(meal.calories).toLong()
            }
            .values
            .map { group ->
                val latest = group.maxWithOrNull(compareBy<Meal> { it.date }.thenBy { it.id }) ?: group.first()
                FrequentMeal(template = latest, count = group.size)
            }
            .sortedWith(compareByDescending<FrequentMeal> { it.count }.thenByDescending { it.template.date })
            .take(limit)
    }

    /** Deterministic summary for a single session, detecting working-weight PRs relative to earlier completed sessions. */
    fun summarizeSession(session: Session, data: FitnessSnapshot): SessionSummary {
        val sessionSets = data.sets.filter { it.sessionId == session.id }
        val working = sessionSets.filter { it.kind == SetKind.WORKING }
        val warmups = sessionSets.count { it.kind == SetKind.WARM_UP }
        val volume = working.sumOf { it.weightKg * it.reps }
        val exercises = working.map { it.exercise }.distinct()
        val muscles = exercises.flatMap { ExerciseMuscles.of(it).primary }.distinct()

        val priorSessionIds = data.sessions
            .filter { it.id != session.id && it.completedAt != null && it.date <= session.date }
            .map { it.id }
            .toSet()
        val priorBestByExercise = data.sets
            .filter { it.kind == SetKind.WORKING && it.sessionId in priorSessionIds }
            .groupBy { it.exercise }
            .mapValues { (_, list) -> list.maxOf { it.weightKg } }

        val newPrs = exercises.filter { ex ->
            val maxNow = working.filter { it.exercise == ex }.maxOfOrNull { it.weightKg } ?: 0.0
            val priorMax = priorBestByExercise[ex]
            maxNow > 0.0 && priorMax != null && maxNow > priorMax
        }
        return SessionSummary(
            workingSets = working.size,
            warmupSets = warmups,
            totalVolumeKg = volume,
            exercisesCount = exercises.size,
            primaryMuscles = muscles,
            newPrExercises = newPrs,
        )
    }

    /** Formats a clean, shareable plain-text workout recap (working sets only; warm-ups stay out of working volume). */
    fun formatRecap(
        title: String,
        dateLabel: String,
        sets: List<TrainingSet>,
        exerciseName: (Exercise) -> String,
        formatLoad: (Double) -> String,
        workingSetsLabel: (Int) -> String,
        volumeLabel: (Double) -> String,
    ): String {
        val working = sets.filter { it.kind == SetKind.WORKING }
        val lines = mutableListOf("Forma · $title ($dateLabel)")
        val ordered = working.map { it.exercise }.distinct()
        for (ex in ordered) {
            val exSets = working.filter { it.exercise == ex }
                .joinToString(", ") { "${formatLoad(it.weightKg)} × ${it.reps}" }
            lines += "• ${exerciseName(ex)}: $exSets"
        }
        val volume = working.sumOf { it.weightKg * it.reps }
        val footer = if (volume > 0.0) "${workingSetsLabel(working.size)} · ${volumeLabel(volume)}" else workingSetsLabel(working.size)
        lines += footer
        return lines.joinToString("\n")
    }
}
