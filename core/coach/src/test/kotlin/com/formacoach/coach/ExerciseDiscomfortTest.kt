package com.formacoach.coach

import com.formacoach.model.Exercise
import com.formacoach.model.ExerciseDiscomfortRegion
import com.formacoach.model.ExerciseDiscomfortSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseDiscomfortTest {
    @Test
    fun defaultEmptyFilterIncludesAllExercises() {
        assertTrue(Exercise.entries.all { ExerciseDiscomfortSelection.includes(it, emptySet()) })
    }

    @Test
    fun excludesPrimaryAndSupportingMusclesForSelectedRegion() {
        // Conventional deadlift maps ERECTORS; leg extension does not.
        assertFalse(ExerciseDiscomfortSelection.includes(Exercise.CONVENTIONAL_DEADLIFT, setOf(ExerciseDiscomfortRegion.LOWER_BACK)))
        assertTrue(ExerciseDiscomfortSelection.includes(Exercise.LEG_EXTENSION, setOf(ExerciseDiscomfortRegion.LOWER_BACK)))

        // Barbell overhead press maps FRONT_DELTS; calf raise does not.
        assertFalse(ExerciseDiscomfortSelection.includes(Exercise.BARBELL_OVERHEAD_PRESS, setOf(ExerciseDiscomfortRegion.SHOULDERS)))
        assertTrue(ExerciseDiscomfortSelection.includes(Exercise.STANDING_CALF_RAISE, setOf(ExerciseDiscomfortRegion.SHOULDERS)))

        // Barbell row maps UPPER_BACK; squat does not.
        assertFalse(ExerciseDiscomfortSelection.includes(Exercise.BARBELL_ROW, setOf(ExerciseDiscomfortRegion.NECK_UPPER_BACK)))
        assertTrue(ExerciseDiscomfortSelection.includes(Exercise.SQUAT, setOf(ExerciseDiscomfortRegion.NECK_UPPER_BACK)))
    }

    @Test
    fun combinesMultipleRegionsDeterministically() {
        val combined = setOf(ExerciseDiscomfortRegion.SHOULDERS, ExerciseDiscomfortRegion.LOWER_BACK)
        assertFalse(ExerciseDiscomfortSelection.includes(Exercise.BARBELL_OVERHEAD_PRESS, combined))
        assertFalse(ExerciseDiscomfortSelection.includes(Exercise.CONVENTIONAL_DEADLIFT, combined))
        assertTrue(ExerciseDiscomfortSelection.includes(Exercise.STANDING_CALF_RAISE, combined))
    }

    @Test
    fun encodeAndDecodeRoundTripAndIgnoreUnknownTokens() {
        val encoded = ExerciseDiscomfortSelection.encode(setOf(ExerciseDiscomfortRegion.SHOULDERS, ExerciseDiscomfortRegion.LOWER_BACK))
        assertEquals("LOWER_BACK,SHOULDERS", encoded)
        assertEquals(
            setOf(ExerciseDiscomfortRegion.LOWER_BACK, ExerciseDiscomfortRegion.SHOULDERS),
            ExerciseDiscomfortSelection.decode("$encoded,UNKNOWN_REGION,,LEGACY"),
        )
        assertEquals(emptySet<ExerciseDiscomfortRegion>(), ExerciseDiscomfortSelection.decode(null))
        assertEquals(emptySet<ExerciseDiscomfortRegion>(), ExerciseDiscomfortSelection.decode(""))
    }
}
