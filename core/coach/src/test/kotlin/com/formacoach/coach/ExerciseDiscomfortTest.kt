package com.formacoach.coach

import com.formacoach.model.DiscomfortMode
import com.formacoach.model.Exercise
import com.formacoach.model.ExerciseDiscomfortRegion
import com.formacoach.model.ExerciseDiscomfortSelection
import com.formacoach.model.ExerciseMuscles
import com.formacoach.model.Muscle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseDiscomfortTest {
    @Test fun noSelectionHidesNothing() {
        assertTrue(Exercise.entries.all { ExerciseDiscomfortSelection.includes(it, emptySet()) })
    }

    @Test fun neckOnlyMatchesUpperTrapWorkAndLeavesRowsAlone() {
        val neck = setOf(ExerciseDiscomfortRegion.NECK)
        assertFalse(ExerciseDiscomfortSelection.includes(Exercise.DUMBBELL_SHRUG, neck))
        assertFalse(ExerciseDiscomfortSelection.includes(Exercise.BARBELL_OVERHEAD_PRESS, neck))
        assertFalse(ExerciseDiscomfortSelection.includes(Exercise.LATERAL_RAISE, neck))
        // Mid-back and rear-delt work stays visible when only the neck is chosen.
        assertTrue(ExerciseDiscomfortSelection.includes(Exercise.ROW, neck))
        assertTrue(ExerciseDiscomfortSelection.includes(Exercise.FACE_PULL, neck))
        assertTrue(ExerciseDiscomfortSelection.includes(Exercise.SQUAT, neck))
    }

    @Test fun upperBackMatchesMidBackDetail() {
        val upper = setOf(ExerciseDiscomfortRegion.UPPER_BACK)
        assertFalse(ExerciseDiscomfortSelection.includes(Exercise.BARBELL_ROW, upper))
        assertTrue(ExerciseDiscomfortSelection.includes(Exercise.DUMBBELL_SHRUG, upper))
        assertTrue(ExerciseDiscomfortSelection.includes(Exercise.LEG_EXTENSION, upper))
    }

    @Test fun shouldersAndLowerBackKeepTheirGroupRules() {
        assertFalse(ExerciseDiscomfortSelection.includes(Exercise.CONVENTIONAL_DEADLIFT, setOf(ExerciseDiscomfortRegion.LOWER_BACK)))
        assertTrue(ExerciseDiscomfortSelection.includes(Exercise.LEG_EXTENSION, setOf(ExerciseDiscomfortRegion.LOWER_BACK)))
        assertFalse(ExerciseDiscomfortSelection.includes(Exercise.BARBELL_OVERHEAD_PRESS, setOf(ExerciseDiscomfortRegion.SHOULDERS)))
        assertTrue(ExerciseDiscomfortSelection.includes(Exercise.STANDING_CALF_RAISE, setOf(ExerciseDiscomfortRegion.SHOULDERS)))
    }

    @Test fun markingKeepsEverythingVisibleAndNamesTheAreas() {
        val chosen = setOf(ExerciseDiscomfortRegion.NECK, ExerciseDiscomfortRegion.SHOULDERS)
        assertTrue(Exercise.entries.all { ExerciseDiscomfortSelection.visible(it, chosen, DiscomfortMode.MARK) })
        assertFalse(ExerciseDiscomfortSelection.visible(Exercise.DUMBBELL_SHRUG, chosen, DiscomfortMode.HIDE))
        assertEquals(listOf(ExerciseDiscomfortRegion.NECK, ExerciseDiscomfortRegion.SHOULDERS), ExerciseDiscomfortSelection.matches(Exercise.BARBELL_OVERHEAD_PRESS, chosen))
        assertEquals(emptyList<ExerciseDiscomfortRegion>(), ExerciseDiscomfortSelection.matches(Exercise.LEG_EXTENSION, chosen))
    }

    @Test fun legacyCombinedAreaKeepsHidingBothAndUnknownTokensAreIgnored() {
        val legacy = ExerciseDiscomfortSelection.decode("NECK_UPPER_BACK,SHOULDERS,UNKNOWN,,")
        assertEquals(setOf(ExerciseDiscomfortRegion.NECK, ExerciseDiscomfortRegion.UPPER_BACK, ExerciseDiscomfortRegion.SHOULDERS), legacy)
        assertEquals(DiscomfortMode.HIDE, ExerciseDiscomfortSelection.decodeMode(null, hasLegacySelection = true))
        assertEquals(DiscomfortMode.MARK, ExerciseDiscomfortSelection.decodeMode(null, hasLegacySelection = false))
        assertEquals(DiscomfortMode.MARK, ExerciseDiscomfortSelection.decodeMode("MARK", hasLegacySelection = true))
        val encoded = ExerciseDiscomfortSelection.encode(legacy)
        assertEquals("NECK,SHOULDERS,UPPER_BACK", encoded)
        assertEquals(legacy, ExerciseDiscomfortSelection.decode(encoded))
        assertEquals(emptySet<ExerciseDiscomfortRegion>(), ExerciseDiscomfortSelection.decode(null))
    }

    @Test fun focusMusclesComeFirstWithoutReorderingWithinGroups() {
        val focus = setOf(Muscle.UPPER_BACK, Muscle.REAR_DELTS)
        val ordered = ExerciseDiscomfortSelection.focusFirst(Exercise.entries, focus)
        val firstOther = ordered.indexOfFirst { !ExerciseDiscomfortSelection.isFocus(it, focus) }
        assertTrue(firstOther > 0)
        assertTrue(ordered.drop(firstOther).none { ExerciseDiscomfortSelection.isFocus(it, focus) })
        assertEquals(Exercise.entries.filter { ExerciseMuscles.of(it).primary.any { m -> m in focus } }, ordered.take(firstOther))
        assertEquals(Exercise.entries, ExerciseDiscomfortSelection.focusFirst(Exercise.entries, emptySet()))
        assertEquals(focus, ExerciseDiscomfortSelection.decodeFocus(ExerciseDiscomfortSelection.encodeFocus(focus) + ",NOPE"))
    }
}
