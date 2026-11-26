package com.formacoach.model

/**
 * User-controlled library preferences, not diagnoses or medical contraindications.
 * Neck uses the catalogued upper-trapezius detail only (shrugs, overhead presses, raises, deadlifts), so choosing it
 * no longer affects mid-back work such as rows. Matching never claims an exercise is unsafe or helpful for symptoms.
 */
enum class ExerciseDiscomfortRegion(val muscles: Set<Muscle> = emptySet(), val details: Set<MuscleDetail> = emptySet()) {
    NECK(details = setOf(MuscleDetail.UPPER_TRAPS)),
    UPPER_BACK(details = setOf(MuscleDetail.MID_BACK)),
    SHOULDERS(muscles = setOf(Muscle.FRONT_DELTS, Muscle.REAR_DELTS)),
    LOWER_BACK(muscles = setOf(Muscle.ERECTORS)),
    ;

    /** True when a catalogued primary or supporting muscle of [exercise] falls in this area. */
    fun loads(exercise: Exercise): Boolean {
        val activation = ExerciseMuscles.of(exercise)
        return (activation.primary + activation.secondary).any { it in muscles } ||
            (activation.primaryDetail + activation.secondaryDetail).any { it in details }
    }
}

/** Marking shows every exercise with a note; hiding removes matching exercises from the library list only. */
enum class DiscomfortMode { MARK, HIDE }

object ExerciseDiscomfortSelection {
    const val STORAGE_KEY: String = "forma_discomfort_exclusions"
    const val MODE_KEY: String = "forma_discomfort_mode"
    const val FOCUS_KEY: String = "forma_focus_muscles"

    /** Areas the user chose that [exercise] loads, in a stable order. */
    fun matches(exercise: Exercise, regions: Set<ExerciseDiscomfortRegion>): List<ExerciseDiscomfortRegion> =
        ExerciseDiscomfortRegion.entries.filter { it in regions && it.loads(exercise) }

    fun includes(exercise: Exercise, excluding: Set<ExerciseDiscomfortRegion>): Boolean =
        excluding.isEmpty() || matches(exercise, excluding).isEmpty()

    /** Whether [exercise] stays in the list: marking never hides anything. */
    fun visible(exercise: Exercise, regions: Set<ExerciseDiscomfortRegion>, mode: DiscomfortMode): Boolean =
        mode == DiscomfortMode.MARK || includes(exercise, regions)

    /**
     * Older builds stored a combined "NECK_UPPER_BACK" area. It decodes to both new areas so nothing the person
     * chose silently disappears; they can clear either one.
     */
    fun decode(stored: String?): Set<ExerciseDiscomfortRegion> {
        if (stored.isNullOrBlank()) return emptySet()
        return stored.split(',').flatMap { token ->
            if (token == "NECK_UPPER_BACK") listOf(ExerciseDiscomfortRegion.NECK, ExerciseDiscomfortRegion.UPPER_BACK)
            else listOfNotNull(ExerciseDiscomfortRegion.entries.firstOrNull { it.name == token })
        }.toSet()
    }

    fun encode(regions: Set<ExerciseDiscomfortRegion>): String =
        regions.map { it.name }.sorted().joinToString(",")

    /** Older builds always hid matches, so a stored selection without a saved mode keeps hiding. */
    fun decodeMode(stored: String?, hasLegacySelection: Boolean): DiscomfortMode =
        DiscomfortMode.entries.firstOrNull { it.name == stored } ?: if (hasLegacySelection) DiscomfortMode.HIDE else DiscomfortMode.MARK

    fun decodeFocus(stored: String?): Set<Muscle> =
        stored.orEmpty().split(',').mapNotNull { token -> Muscle.entries.firstOrNull { it.name == token } }.toSet()

    fun encodeFocus(muscles: Set<Muscle>): String = muscles.map { it.name }.sorted().joinToString(",")

    /** Muscles the person chose to train more come first; the order within each group is unchanged. */
    fun focusFirst(exercises: List<Exercise>, focus: Set<Muscle>): List<Exercise> =
        if (focus.isEmpty()) exercises
        else exercises.sortedBy { exercise -> if (ExerciseMuscles.of(exercise).primary.any { it in focus }) 0 else 1 }

    fun isFocus(exercise: Exercise, focus: Set<Muscle>): Boolean = ExerciseMuscles.of(exercise).primary.any { it in focus }
}
