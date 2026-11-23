package com.formacoach.model

/**
 * User-controlled library exclusion preferences, not diagnoses or medical contraindications.
 * The catalog has no distinct neck muscle, so neck/upper-back uses the upper-back association only.
 */
enum class ExerciseDiscomfortRegion(val directlyTargetedMuscles: Set<Muscle>) {
    NECK_UPPER_BACK(setOf(Muscle.UPPER_BACK)),
    SHOULDERS(setOf(Muscle.FRONT_DELTS, Muscle.REAR_DELTS)),
    LOWER_BACK(setOf(Muscle.ERECTORS)),
}

object ExerciseDiscomfortSelection {
    const val STORAGE_KEY: String = "forma_discomfort_exclusions"

    fun includes(exercise: Exercise, excluding: Set<ExerciseDiscomfortRegion>): Boolean {
        if (excluding.isEmpty()) return true
        val excludedMuscles = excluding.flatMapTo(mutableSetOf()) { it.directlyTargetedMuscles }
        val activation = ExerciseMuscles.of(exercise)
        return (activation.primary + activation.secondary).none { it in excludedMuscles }
    }

    fun decode(stored: String?): Set<ExerciseDiscomfortRegion> {
        if (stored.isNullOrBlank()) return emptySet()
        return stored.split(',')
            .mapNotNull { token -> ExerciseDiscomfortRegion.entries.firstOrNull { it.name == token } }
            .toSet()
    }

    fun encode(regions: Set<ExerciseDiscomfortRegion>): String =
        regions.map { it.name }.sorted().joinToString(",")
}
