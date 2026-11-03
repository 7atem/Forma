@file:kotlinx.serialization.UseSerializers(com.formacoach.model.LocalDateSerializer::class)

package com.formacoach.model

import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable

enum class Goal { FAT_LOSS, BUILD_STRENGTH, BUILD_HABIT }

@Serializable
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

@Serializable
data class Session(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val templateId: String,
    val completedAt: Long? = null,
    val label: String = "",
    val plannedExercises: List<Exercise> = emptyList(),
    val restSeconds: Int = 90,
) {
    init {
        require(id.isNotBlank() && id.length <= 80)
        require(templateId.isNotBlank() && templateId.length <= 80)
        require(label.length <= 120)
        require(plannedExercises.size <= 20 && plannedExercises.distinct().size == plannedExercises.size)
        require(restSeconds in 15..600)
    }
}

@Serializable
data class TrainingSet(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val exercise: Exercise,
    val reps: Int,
    val weightKg: Double,
) {
    init {
        require(id.isNotBlank() && id.length <= 80 && sessionId.isNotBlank() && sessionId.length <= 80)
        require(reps in 1..1000)
        require(weightKg.isFinite() && weightKg in 0.0..1000.0)
    }
}

/** All nutrient numbers describe the whole logged portion, explicitly supplied by the user. */
@Serializable
data class Meal(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val foodId: String? = null,
    val quantity: Double? = null,
    val unit: NutritionUnit? = null,
    val source: String = "",
) {
    init {
        require(id.isNotBlank() && id.length <= 80)
        require(name.isNotBlank() && name.length <= 120)
        require(calories.isFinite() && calories in 0.0..20000.0)
        require(listOf(protein, carbs, fat).all { it.isFinite() && it in 0.0..2000.0 })
        require(source.length <= 4000)
        require(quantity == null || quantity.isFinite() && quantity > 0 && quantity <= 100000)
        require((quantity == null) == (unit == null))
        require(foodId == null || foodId.isNotBlank() && foodId.length <= 80)
    }
}

/** One observation per local calendar date; unknown values remain null. */
@Serializable
data class Measurement(val date: LocalDate, val weightKg: Double?, val waistCm: Double?) {
    init {
        require(weightKg != null || waistCm != null)
        require(weightKg == null || weightKg.isFinite() && weightKg > 0 && weightKg <= 1000)
        require(waistCm == null || waistCm.isFinite() && waistCm > 0 && waistCm <= 500)
    }
}

@Serializable
data class FitnessSnapshot(
    val profile: Profile? = null,
    val sessions: List<Session> = emptyList(),
    val sets: List<TrainingSet> = emptyList(),
    val meals: List<Meal> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
    val programs: List<TrainingProgram> = emptyList(),
    val foods: List<Food> = emptyList(),
    val checkIns: List<WeeklyCheckIn> = emptyList(),
    val proposals: List<PlanProposal> = emptyList(),
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
