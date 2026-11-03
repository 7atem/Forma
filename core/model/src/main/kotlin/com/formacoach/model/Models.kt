package com.formacoach.model

import java.time.LocalDate
import java.util.UUID

enum class Goal { FAT_LOSS, BUILD_STRENGTH, BUILD_HABIT }

data class Profile(
    val name: String,
    val goal: Goal,
    val sessionsPerWeek: Int,
    val caloriesTarget: Int? = null,
    val proteinTarget: Double? = null,
) {
    init {
        require(name.isNotBlank() && name.length <= 80)
        require(sessionsPerWeek in 2..4)
        require(caloriesTarget == null || caloriesTarget in 1..20000)
        require(proteinTarget == null || proteinTarget.isFinite() && proteinTarget > 0 && proteinTarget <= 1000)
    }
}

enum class Exercise { SQUAT, LEG_PRESS, CHEST_PRESS, ROW, LAT_PULLDOWN, HIP_HINGE, SHOULDER_PRESS, SPLIT_SQUAT }

data class TrainingTemplate(val id: String, val exercises: List<Exercise>)

object StarterTemplates {
    val all = listOf(
        TrainingTemplate("A", listOf(Exercise.SQUAT, Exercise.CHEST_PRESS, Exercise.ROW)),
        TrainingTemplate("B", listOf(Exercise.LEG_PRESS, Exercise.LAT_PULLDOWN, Exercise.HIP_HINGE)),
    )
}

data class Session(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val templateId: String,
    val completedAt: Long? = null,
) {
    init { require(templateId in StarterTemplates.all.map { it.id }) }
}

data class TrainingSet(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val exercise: Exercise,
    val reps: Int,
    val weightKg: Double,
) {
    init {
        require(reps in 1..1000)
        require(weightKg.isFinite() && weightKg in 0.0..1000.0)
    }
}

/** All nutrient numbers describe the whole logged portion, explicitly supplied by the user. */
data class Meal(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
) {
    init {
        require(name.isNotBlank() && name.length <= 120)
        require(calories.isFinite() && calories in 0.0..20000.0)
        require(listOf(protein, carbs, fat).all { it.isFinite() && it in 0.0..2000.0 })
    }
}

/** One observation per local calendar date; unknown values remain null. */
data class Measurement(val date: LocalDate, val weightKg: Double?, val waistCm: Double?) {
    init {
        require(weightKg != null || waistCm != null)
        require(weightKg == null || weightKg.isFinite() && weightKg > 0 && weightKg <= 1000)
        require(waistCm == null || waistCm.isFinite() && waistCm > 0 && waistCm <= 500)
    }
}

data class FitnessSnapshot(
    val profile: Profile? = null,
    val sessions: List<Session> = emptyList(),
    val sets: List<TrainingSet> = emptyList(),
    val meals: List<Meal> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
)

/** Accept Latin, Arabic-Indic and Persian digits, with Arabic decimal separator. */
object NumericInput {
    fun decimal(raw: String): Double? = normalize(raw).toDoubleOrNull()?.takeIf { it.isFinite() }
    fun integer(raw: String): Int? = normalize(raw).toIntOrNull()
    private fun normalize(raw: String): String = buildString {
        raw.trim().forEach { char ->
            when {
                char == '\u066b' -> append('.')
                Character.digit(char, 10) >= 0 -> append(Character.digit(char, 10))
                else -> append(char)
            }
        }
    }
}
