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
    val carbsTarget: Double? = null,
    val fatTarget: Double? = null,
    val waterGoalMl: Int? = null,
) {
    init {
        require(name.length <= 80) // optional: a blank name only changes the greeting
        require(sessionsPerWeek in 2..4)
        require(caloriesTarget == null || caloriesTarget in 1..20000)
        require(proteinTarget == null || proteinTarget.isFinite() && proteinTarget > 0 && proteinTarget <= 1000)
        require(carbsTarget == null || carbsTarget.isFinite() && carbsTarget > 0 && carbsTarget <= 2000)
        require(fatTarget == null || fatTarget.isFinite() && fatTarget > 0 && fatTarget <= 1000)
        require(waterGoalMl == null || waterGoalMl in 250..15000)
    }
}

enum class SetKind { WORKING, WARM_UP }
enum class SessionSource { LOCAL, APPLE_WATCH_IMPORT }

data class TrainingTemplate(val id: String, val exercises: List<Exercise>)

object StarterTemplates {
    val all = listOf(
        TrainingTemplate("A", listOf(Exercise.SQUAT, Exercise.CHEST_PRESS, Exercise.ROW)),
        TrainingTemplate("B", listOf(Exercise.LEG_PRESS, Exercise.LAT_PULLDOWN, Exercise.HIP_HINGE)),
        TrainingTemplate("HOME_EXPRESS", listOf(Exercise.SQUAT, Exercise.PUSH_UP, Exercise.GLUTE_BRIDGE, Exercise.DEAD_BUG)),
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
    val source: SessionSource = SessionSource.LOCAL,
    val averageHeartRateBpm: Double? = null,
    val activeEnergyKcal: Double? = null,
    val healthKitWorkoutId: String? = null,
    val prescriptions:List<ExercisePrescription> = emptyList(),
) {
    init {
        require(id.isNotBlank() && id.length <= 80)
        require(templateId.isNotBlank() && templateId.length <= 80)
        require(label.length <= 120)
        require(plannedExercises.size <= 20 && plannedExercises.distinct().size == plannedExercises.size)
        require(restSeconds in 15..600)
        require(averageHeartRateBpm == null || averageHeartRateBpm.isFinite() && averageHeartRateBpm in 1.0..300.0)
        require(activeEnergyKcal == null || activeEnergyKcal.isFinite() && activeEnergyKcal in 0.0..100000.0)
        require(healthKitWorkoutId == null || healthKitWorkoutId.isNotBlank() && healthKitWorkoutId.length <= 80)
        PlanDetails.validate(plannedExercises,prescriptions)
    }
}

@Serializable
data class TrainingSet(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val exercise: Exercise,
    val reps: Int,
    val weightKg: Double,
    val kind: SetKind = SetKind.WORKING,
    val rpe: Double? = null,
) {
    init {
        require(id.isNotBlank() && id.length <= 80 && sessionId.isNotBlank() && sessionId.length <= 80)
        require(reps in 1..1000)
        require(weightKg.isFinite() && weightKg in 0.0..1000.0)
        require(rpe == null || rpe.isFinite() && rpe in 0.0..10.0)
    }
}

/** All nutrient numbers describe the whole logged portion, explicitly supplied by the user. */
enum class MealSlot { BREAKFAST, LUNCH, DINNER, SNACK, SUHOOR, IFTAR, UNSPECIFIED }

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
    val slot:MealSlot = MealSlot.UNSPECIFIED,
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
data class Measurement(
    val date: LocalDate,
    val weightKg: Double?,
    val waistCm: Double?,
    val neckCm: Double? = null,
    val chestCm: Double? = null,
    val armsCm: Double? = null,
    val hipsCm: Double? = null,
    val thighsCm: Double? = null,
) {
    init {
        val cmValues = listOf(waistCm, neckCm, chestCm, armsCm, hipsCm, thighsCm)
        require(weightKg != null || cmValues.any { it != null })
        require(weightKg == null || weightKg.isFinite() && weightKg > 0 && weightKg <= 1000)
        require(cmValues.all { it == null || it.isFinite() && it > 0 && it <= 500 })
    }
}

/** One daily logged hydration total (in milliliters) per local calendar date. */
@Serializable
data class Hydration(val date: LocalDate, val milliliters: Int) {
    init {
        require(milliliters in 0..20000)
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
    val exercisePreferences: List<ExercisePreference> = emptyList(),
    val hydration: List<Hydration> = emptyList(),
    val customExercises: List<CustomExercise> = emptyList(),
    val customSets: List<CustomExerciseSet> = emptyList(),
    val supplements: List<Supplement> = emptyList(),
    val supplementIntakes: List<SupplementIntake> = emptyList(),
)

/** A user-defined exercise outside the reviewed catalog; muscle and equipment are the user's own labels. */
@Serializable
data class CustomExercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val equipment: Equipment,
    val primaryMuscle: Muscle,
    val notes: String = "",
) {
    init {
        require(id.isNotBlank() && id.length <= 64)
        require(name.isNotBlank() && name.length <= 60)
        require(notes.length <= 120)
    }
}

/** One recorded set for a custom exercise. Warm-ups stay out of personal bests. */
@Serializable
data class CustomExerciseSet(
    val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val date: LocalDate,
    val reps: Int,
    val weightKg: Double,
    val warmUp: Boolean = false,
) {
    init {
        require(id.isNotBlank() && id.length <= 64 && exerciseId.isNotBlank())
        require(reps in 1..1000)
        require(weightKg.isFinite() && weightKg in 0.0..1000.0)
    }
}

/** A user-entered supplement name and optional free-text dose label; Forma never suggests doses. */
@Serializable
data class Supplement(val id: String = UUID.randomUUID().toString(), val name: String, val dose: String = "") {
    init {
        require(id.isNotBlank() && id.length <= 64)
        require(name.isNotBlank() && name.length <= 60)
        require(dose.length <= 30)
    }
}

/** The user checked a supplement off on a local calendar date. Missing check-offs are unknown, not misses. */
@Serializable
data class SupplementIntake(val date: LocalDate, val supplementId: String) {
    init { require(supplementId.isNotBlank()) }
}

@Serializable
data class ExercisePreference(val exercise:Exercise,val favorite:Boolean=false,val notes:String="") {
    init {require(notes.length<=2000)}
}

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

/** A body-weight reading copied read-only from Health Connect. Kept apart from Forma measurements. */
data class ImportedWeight(val id: String, val date: LocalDate, val weightKg: Double, val source: String) {
    init {
        require(id.isNotBlank() && id.length <= 128)
        require(weightKg.isFinite() && weightKg > 0 && weightKg <= 1000)
        require(source.length <= 200)
    }
}

/** A workout session copied read-only from Health Connect. Never counted as a Forma session. */
data class ImportedWorkout(val id: String, val date: LocalDate, val startEpochMs: Long, val endEpochMs: Long, val exerciseType: Int, val title: String, val source: String) {
    init {
        require(id.isNotBlank() && id.length <= 128)
        require(endEpochMs >= startEpochMs)
        require(title.length <= 200 && source.length <= 200)
    }
    val minutes: Long get() = (endEpochMs - startEpochMs) / 60_000
}

data class HealthImports(val weights: List<ImportedWeight> = emptyList(), val workouts: List<ImportedWorkout> = emptyList())
