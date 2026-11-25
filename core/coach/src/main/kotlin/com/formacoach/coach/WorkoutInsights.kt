package com.formacoach.coach

import com.formacoach.model.*

/** One frequently logged meal template for 1-tap quick re-logging. */
data class FrequentMeal(
    val template: Meal,
    val count: Int,
)

object WorkoutInsights {
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
