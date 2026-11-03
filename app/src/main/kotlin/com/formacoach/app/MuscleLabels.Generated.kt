package com.formacoach.app

import com.formacoach.model.Muscle

internal fun muscleLabel(muscle: Muscle): Int = when(muscle) {
    Muscle.CHEST -> R.string.muscle_chest
    Muscle.FRONT_DELTS -> R.string.muscle_front_delts
    Muscle.REAR_DELTS -> R.string.muscle_rear_delts
    Muscle.LATS -> R.string.muscle_lats
    Muscle.UPPER_BACK -> R.string.muscle_upper_back
    Muscle.BICEPS -> R.string.muscle_biceps
    Muscle.TRICEPS -> R.string.muscle_triceps
    Muscle.CORE -> R.string.muscle_core
    Muscle.GLUTES -> R.string.muscle_glutes
    Muscle.QUADRICEPS -> R.string.muscle_quadriceps
    Muscle.HAMSTRINGS -> R.string.muscle_hamstrings
    Muscle.CALVES -> R.string.muscle_calves
    Muscle.ERECTORS -> R.string.muscle_erectors
    Muscle.ADDUCTORS -> R.string.muscle_adductors
}
