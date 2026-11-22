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

internal fun exerciseLabel(exercise:com.formacoach.model.Exercise):Int=when(exercise) {
    com.formacoach.model.Exercise.SQUAT->R.string.catalog_exercise_squat
    com.formacoach.model.Exercise.LEG_PRESS->R.string.catalog_exercise_leg_press
    com.formacoach.model.Exercise.CHEST_PRESS->R.string.catalog_exercise_chest_press
    com.formacoach.model.Exercise.ROW->R.string.catalog_exercise_row
    com.formacoach.model.Exercise.LAT_PULLDOWN->R.string.catalog_exercise_lat_pulldown
    com.formacoach.model.Exercise.HIP_HINGE->R.string.catalog_exercise_hip_hinge
    com.formacoach.model.Exercise.SHOULDER_PRESS->R.string.catalog_exercise_shoulder_press
    com.formacoach.model.Exercise.SPLIT_SQUAT->R.string.catalog_exercise_split_squat
    com.formacoach.model.Exercise.GOBLET_SQUAT->R.string.catalog_exercise_goblet_squat
    com.formacoach.model.Exercise.BARBELL_BACK_SQUAT->R.string.catalog_exercise_barbell_back_squat
    com.formacoach.model.Exercise.REVERSE_LUNGE->R.string.catalog_exercise_reverse_lunge
    com.formacoach.model.Exercise.STEP_UP->R.string.catalog_exercise_step_up
    com.formacoach.model.Exercise.LEG_EXTENSION->R.string.catalog_exercise_leg_extension
    com.formacoach.model.Exercise.SEATED_LEG_CURL->R.string.catalog_exercise_seated_leg_curl
    com.formacoach.model.Exercise.STANDING_CALF_RAISE->R.string.catalog_exercise_standing_calf_raise
    com.formacoach.model.Exercise.SEATED_CALF_RAISE->R.string.catalog_exercise_seated_calf_raise
    com.formacoach.model.Exercise.BARBELL_HIP_THRUST->R.string.catalog_exercise_barbell_hip_thrust
    com.formacoach.model.Exercise.GLUTE_BRIDGE->R.string.catalog_exercise_glute_bridge
    com.formacoach.model.Exercise.BARBELL_ROMANIAN_DEADLIFT->R.string.catalog_exercise_barbell_romanian_deadlift
    com.formacoach.model.Exercise.DUMBBELL_BENCH_PRESS->R.string.catalog_exercise_dumbbell_bench_press
    com.formacoach.model.Exercise.INCLINE_DUMBBELL_PRESS->R.string.catalog_exercise_incline_dumbbell_press
    com.formacoach.model.Exercise.PUSH_UP->R.string.catalog_exercise_push_up
    com.formacoach.model.Exercise.KNEE_PUSH_UP->R.string.catalog_exercise_knee_push_up
    com.formacoach.model.Exercise.CABLE_CHEST_FLY->R.string.catalog_exercise_cable_chest_fly
    com.formacoach.model.Exercise.SINGLE_ARM_DUMBBELL_ROW->R.string.catalog_exercise_single_arm_dumbbell_row
    com.formacoach.model.Exercise.CHEST_SUPPORTED_ROW->R.string.catalog_exercise_chest_supported_row
    com.formacoach.model.Exercise.PULL_UP->R.string.catalog_exercise_pull_up
    com.formacoach.model.Exercise.FACE_PULL->R.string.catalog_exercise_face_pull
    com.formacoach.model.Exercise.DUMBBELL_SHOULDER_PRESS->R.string.catalog_exercise_dumbbell_shoulder_press
    com.formacoach.model.Exercise.LATERAL_RAISE->R.string.catalog_exercise_lateral_raise
    com.formacoach.model.Exercise.REAR_DELT_FLY->R.string.catalog_exercise_rear_delt_fly
    com.formacoach.model.Exercise.DUMBBELL_SHRUG->R.string.catalog_exercise_dumbbell_shrug
    com.formacoach.model.Exercise.DUMBBELL_CURL->R.string.catalog_exercise_dumbbell_curl
    com.formacoach.model.Exercise.HAMMER_CURL->R.string.catalog_exercise_hammer_curl
    com.formacoach.model.Exercise.MACHINE_PREACHER_CURL->R.string.catalog_exercise_machine_preacher_curl
    com.formacoach.model.Exercise.TRICEPS_PUSHDOWN->R.string.catalog_exercise_triceps_pushdown
    com.formacoach.model.Exercise.OVERHEAD_TRICEPS_EXTENSION->R.string.catalog_exercise_overhead_triceps_extension
    com.formacoach.model.Exercise.DEAD_BUG->R.string.catalog_exercise_dead_bug
    com.formacoach.model.Exercise.REVERSE_CRUNCH->R.string.catalog_exercise_reverse_crunch
    com.formacoach.model.Exercise.CABLE_CRUNCH->R.string.catalog_exercise_cable_crunch
    com.formacoach.model.Exercise.BARBELL_BENCH_PRESS->R.string.catalog_exercise_barbell_bench_press
    com.formacoach.model.Exercise.INCLINE_BARBELL_PRESS->R.string.catalog_exercise_incline_barbell_press
    com.formacoach.model.Exercise.DIP->R.string.catalog_exercise_dip
    com.formacoach.model.Exercise.CONVENTIONAL_DEADLIFT->R.string.catalog_exercise_conventional_deadlift
    com.formacoach.model.Exercise.BARBELL_ROW->R.string.catalog_exercise_barbell_row
    com.formacoach.model.Exercise.BARBELL_OVERHEAD_PRESS->R.string.catalog_exercise_barbell_overhead_press
    com.formacoach.model.Exercise.CABLE_LATERAL_RAISE->R.string.catalog_exercise_cable_lateral_raise
    com.formacoach.model.Exercise.BARBELL_CURL->R.string.catalog_exercise_barbell_curl
    com.formacoach.model.Exercise.SKULL_CRUSHER->R.string.catalog_exercise_skull_crusher
    com.formacoach.model.Exercise.HANGING_LEG_RAISE->R.string.catalog_exercise_hanging_leg_raise
}

internal fun equipmentLabel(value:com.formacoach.model.Equipment):Int=when(value) {
    com.formacoach.model.Equipment.BODYWEIGHT->R.string.equipment_bodyweight
    com.formacoach.model.Equipment.DUMBBELLS->R.string.equipment_dumbbells
    com.formacoach.model.Equipment.BARBELL->R.string.equipment_barbell
    com.formacoach.model.Equipment.CABLE->R.string.equipment_cable
    com.formacoach.model.Equipment.MACHINE->R.string.equipment_machine
    com.formacoach.model.Equipment.BENCH->R.string.equipment_bench
    com.formacoach.model.Equipment.BOX->R.string.equipment_box
    com.formacoach.model.Equipment.PULL_UP_BAR->R.string.equipment_pull_up_bar
}
